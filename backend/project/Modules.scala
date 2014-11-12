import sbt._

object Modules extends Build {
  lazy val root = RootModule.project

  lazy val model = KitchenModel.project
  lazy val datastore = KitchenDatastore.project
  lazy val rest = KitchenRest.project
  lazy val server = KitchenServer.project
  lazy val serverApi = KitchenServerApi.project
}
