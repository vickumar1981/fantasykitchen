import sbt._
import Keys._
import Libraries._
import Modules._
import sbtassembly.Plugin.AssemblyKeys._

object KitchenServer extends BaseModule {
  val moduleName = "kitchen-server"
  val location = "./server"

  val settings = Seq (
    mainClass in assembly := Some("com.kitchenfantasy.server.Main")
  )

  def project = baseProject dependsOn (serverApi, rest)

  lazy val libraries = Seq (jetty.server, servletApi, commonsIO, typesafeConfig, riakClient, jodaConvert, akkaActor)
}

