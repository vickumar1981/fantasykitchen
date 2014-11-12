import sbt._
import Keys._
import net.virtualvoid.sbt.graph.{Plugin => GraphPlugin}
import de.johoop.cpd4sbt.CopyPasteDetector
import de.johoop.jacoco4sbt._
import JacocoPlugin._
import sbtassembly.Plugin._
import AssemblyKeys._


object MyDefaults {
  lazy val settings =
    assemblySettings ++
    myAssemblySettings ++
    Defaults.defaultSettings ++
    CopyPasteDetector.cpdSettings ++
    jacoco.settings ++
    GraphPlugin.graphSettings ++ Seq (
      organization := "com.kitchenfantasy",
      version      := Versions.mine,
      scalaVersion := Versions.scala
    ) ++ Seq (
      scalacOptions += "-feature",
      scalacOptions += "-unchecked",
      scalacOptions += "-deprecation",
      scalacOptions += "-Xlint",
      scalacOptions += "-Ywarn-dead-code",
      scalacOptions += "-language:_",
      scalacOptions += "-target:jvm-1.7",
      scalacOptions += "-encoding",
      scalacOptions += "UTF-8"
    )

  lazy val myAssemblySettings =
    (mergeStrategy in assembly) <<= (mergeStrategy in assembly) { old =>
      {
        case "application.conf" => MergeStrategy.discard
        case "about.html" => MergeStrategy.discard
        case x => old(x)
      }
    }

  lazy val testingLibs =
    Libraries.defaultTestingLibs
}
