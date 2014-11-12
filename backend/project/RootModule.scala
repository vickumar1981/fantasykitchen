import sbt._
import Keys._
import Modules._

object RootModule {

  lazy val project = Project (
    id = "kitchen",
    base = file("."),
    settings = moduleSettings
  ) aggregate (
    model,
    rest,
    serverApi,
    server
  )

  val moduleSettings =
    MyDefaults.settings ++ Seq (
      name := "project"
    )
}
