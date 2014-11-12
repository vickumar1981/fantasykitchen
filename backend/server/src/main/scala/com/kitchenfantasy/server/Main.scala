package com.kitchenfantasy.server

import java.io.File
import java.lang.reflect.Constructor

import scala.collection.JavaConversions.asScalaBuffer

import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.thread.QueuedThreadPool

import com.basho.riak.client.IRiakClient
import com.basho.riak.client.RiakFactory
import com.basho.riak.client.raw.pbc.PBClientConfig
import com.basho.riak.client.raw.pbc.PBClusterConfig

import com.kitchenfantasy.server.api.RestGroup
import com.kitchenfantasy.server.migration.MigrationRunner
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object Main {

  val Port: String = "http.port"
  val Interface: String = "http.interface"

  val RiakPort: String = "riak.port"
  val RiakHosts: String = "riak.hosts"
  val RiakMaxConnections: String = "riak.maxConnections"

  val MaxThreads: String = "http.threadPool.maxThreads"

  val HandlersPackage: String = "handlers.package"
  val HandlersGroups: String = "handlers.groups"

  def main(args: Array[String]) {
    val config: Config = loadConfiguration(args)

    val handlers = handlersFromConfig(config)
    val publicServer = buildServer(config, handlers)
    val riakClient = setupRiakClient(config)

    DataProvider.configure(riakClient)
    MigrationRunner.initialize(riakClient)

    if (args.length > 1) {
      if (args(1).toLowerCase.equals("--import-products"))
        MigrationRunner.importProducts()
      else
        println("invalid import option: " + args(1))
    }
    else {
      publicServer.start()
      publicServer.join()
    }
  }

  def setupRiakClient(config: Config): IRiakClient = {
    val maxConnections = config.getInt(RiakMaxConnections)
    val port = config.getInt(RiakPort)
    val hosts = config.getStringList(RiakHosts)


    val clusterConfig = new PBClusterConfig(maxConnections)
    val clientConfig = new PBClientConfig.Builder().
      withPort(port).build()

    for (host <- hosts)
      clusterConfig.addHosts(clientConfig, host)

    RiakFactory.newClient(clusterConfig)
  }

  private def buildServer(config: Config, handlers: Seq[RestGroup]): Server = {
    val threadPool = new QueuedThreadPool(config.getInt(MaxThreads))

    val publicServer = new Server(threadPool)
    publicServer.manage(threadPool)
    publicServer.setDumpAfterStart(false)
    publicServer.setDumpBeforeStop(false)

    val httpConfig = new HttpConfiguration()
    httpConfig.setOutputBufferSize(32768)

    // TODO secure
    val public = new ServerConnector(publicServer, new HttpConnectionFactory(httpConfig))
    public.setPort(config.getInt(Port))
    public.setIdleTimeout(30000)
    public.setHost(config.getString(Interface))
    public.setName("public")

    publicServer.setConnectors(Array(public))
    publicServer.setHandler(new ServerHandler(config, handlers))

    publicServer
  }

  private def loadConfiguration(args: Array[String]): Config = {
    require((args.length >= 1) && (args.length <= 2),
      "The server requires ONE argument, the config file, and optionally a --migrate flag.")

    val configFile = new File(args(0))
    require(configFile.exists(), s"The argument provided '${args(0)}' refers to a file that doesn't exist.")

    val config = ConfigFactory.parseFile(configFile)
    verifyConfiguration(config)

    GlobalConfiguration.replace(config)

    config
  }

  private def verifyConfiguration(config: Config) {
    verify(config).containsInt(Port)
    verify(config).containsString(Interface)

    verify(config).containsInt(RiakPort)
    verify(config).containsStringList(RiakHosts)
    verify(config).containsInt(RiakMaxConnections)

    verify(config).containsInt(MaxThreads)

    verify(config).containsString(HandlersPackage)
    verify(config).containsStringList(HandlersGroups)
  }

  private def verify(config: Config) = new {
    def containsInt(key: String) {
      require(doesNotThrow {
        config.getInt(key)
      }, s"Config at `$key` not found or not an Int")
    }

    def containsString(key: String) {
      require(doesNotThrow {
        config.getString(key)
      }, s"Config at `$key` not found or not a String")
    }

    def containsStringList(key: String) {
      require(doesNotThrow {
        config.getStringList(key)
      }, s"Config at `$key` not found or not a String List")
    }
  }

  private def doesNotThrow(thunk: => Any): Boolean = {
    try {
      val _ = thunk
      true
    } catch {
      case x: Throwable => throw x
    }
  }

  private def handlersFromConfig(config: Config): Seq[RestGroup] = {
    val classes = handlerClasses(config)
    verifyHandlerClasses(classes)

    classes.map { clazz =>
      val ctor = getDefaultConstructor(clazz).get
      ctor.setAccessible(true)
      ctor.newInstance().asInstanceOf[RestGroup]
    }
  }

  private def verifyHandlerClasses(classes: Seq[Class[_]]) {
    val errors = classes map {
      verifyHandler(_)
    } collect {
      case (clz, Some(error)) => (clz, error)
    }

    if (errors.length > 0) {
      throw new RuntimeException(messageForErrors(errors))
    }
  }

  private def messageForErrors(errors: Seq[(Class[_], String)]): String = {
    val errorLines = errors.map { tuple =>
      val (clz, err) = tuple
      s"\t- for ${clz.getName}, $err ;"
    }.mkString("\n")

    "Couldn't start server due to handler initialization issues:\n" +
      errorLines +
      "\nPlease fix these issues and restart the server."
  }

  private def handlerClasses(config: Config) = {
    handlerClassNames(config) map {
      Class.forName(_)
    }
  }

  private def handlerClassNames(config: Config) = {
    val handlerPackage = config.getString(HandlersPackage)
    val handlerClassNames = config.getStringList(HandlersGroups)

    handlerClassNames map {
      handlerPackage + "." + _
    }
  }

  private def getDefaultConstructor(handlerClass: Class[_]): Option[Constructor[_]] = {
    val ctors = handlerClass.getConstructors

    ctors find {
      _.getParameterTypes.size == 0
    }
  }

  private def verifyHandler(handlerClass: Class[_]): (Class[_], Option[String]) = {
    if (classOf[RestGroup].isAssignableFrom(handlerClass))
      verifyDefaultConstructor(handlerClass)
    else
      (handlerClass, Some(s"doesn't implement ${classOf[RestGroup].getName}"))
  }

  private def verifyDefaultConstructor(handlerClass: Class[_]): (Class[_], Option[String]) =
    getDefaultConstructor(handlerClass) match {
      case None => (handlerClass, Some("no 0 arg constructor found"))
      case Some(_) => (handlerClass, None)
    }

}
