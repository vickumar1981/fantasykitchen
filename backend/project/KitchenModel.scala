import sbt._
import Keys._
import Libraries._
import Modules._

object KitchenModel extends BaseModule {
  val moduleName = "kitchen-model"
  val location = "./model"

  val settings = Seq ()

  def project = baseProject dependsOn serverApi

  lazy val libraries = Seq (jbCrypt)
}
