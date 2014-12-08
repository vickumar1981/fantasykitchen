import sbt._

object Libraries {

  lazy val jbCrypt = "org.mindrot" % "jbcrypt" % Versions.jbCrypt
  lazy val scalaz = "org.scalaz" %% "scalaz-core" % Versions.scalaz
  lazy val typesafeConfig = "com.typesafe" % "config" % Versions.typesafeConfig

  lazy val logback = "ch.qos.logback" % "logback-classic" % Versions.logback
  lazy val paypalCore = "com.paypal.sdk" % "paypal-core" % Versions.paypalCore
  lazy val paypalApi = "com.paypal.sdk" % "rest-api-sdk" % Versions.paypalApi
  lazy val javaxMail = "com.sun.mail" % "javax.mail" % Versions.javaxMail

  lazy val servletApi = "org.eclipse.jetty.orbit" % "javax.servlet" % Versions.servletApi
  lazy val commonsIO = "org.apache.commons" % "commons-io" % Versions.commonsIO
  lazy val liftJson = "net.liftweb" %% "lift-json" % Versions.liftJson

  lazy val riakClient = "com.basho.riak" % "riak-client" % Versions.riakClient
  
  lazy val jodaConvert = "org.joda" % "joda-convert" % Versions.jodaConvert

  lazy val akkaActor = "com.typesafe.akka" % "akka-actor_2.11" % Versions.akkaActor

  object jetty {
    lazy val server = "org.eclipse.jetty" % "jetty-server" % Versions.jettyServer
  }

  object test {
    lazy val scalaTest = "org.scalatest" %% "scalatest" % Versions.scalaTest
    lazy val mockito = "org.mockito" % "mockito-all" % Versions.mockito
  }

  lazy val defaultTestingLibs = {
    import test._

    def inTest(module: ModuleID) = module % "test"

    Seq (
      inTest(scalaTest),
      inTest(mockito)
    )
  }

}
