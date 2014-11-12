import sbt._
import Keys._
import Libraries._

object KitchenServerApi extends BaseModule {
  val moduleName = "kitchen-server-api"
  val location = "./server-api"

  val settings = Seq ()

  def project = baseProject

  lazy val libraries = Seq (servletApi, liftJson, typesafeConfig, riakClient, jodaConvert)
}

