import sbt._
import Keys._
import Libraries._
import Modules._

object KitchenDatastore extends BaseModule {
  val moduleName = "kitchen-datastore"
  val location = "./datastore"

  val settings = Seq ()

  def project = baseProject dependsOn (model)

  lazy val libraries = Seq (riakClient, jodaConvert)
}

