import sbt._
import Keys._
import Libraries._
import Modules._

object KitchenRest extends BaseModule {
  val moduleName = "kitchen-rest"
  val location = "./rest"

  val settings = Seq ()

  def project = baseProject dependsOn (serverApi, model, datastore)

  lazy val libraries = Seq (typesafeConfig, akkaActor, javaxMail, paypalCore, paypalApi, logback)
}

