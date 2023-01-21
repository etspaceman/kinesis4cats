import sbt._
import sbt.Keys._

import org.typelevel.sbt._
import LibraryDependencies._

object Kinesis4CatsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires: Plugins = TypelevelPlugin

  import TypelevelVersioningPlugin.autoImport._
  import TypelevelGitHubPlugin.autoImport._
  import scalafix.sbt.ScalafixPlugin.autoImport._
  
  val Scala212 = "2.12.17"
  val Scala213 = "2.13.10"
  val Scala3 = "3.2.0"

  val MUnitFramework = new TestFramework("munit.Framework")

  override def buildSettings = Seq(
    tlBaseVersion := "0.0",
    organization := "etspaceman",
    organizationName := "etspaceman",
    licenses := Seq(License.Apache2),
    developers := List(
      // your GitHub handle and name
      tlGitHubDev("etspaceman", "Eric Meisel")
    )
  )

  override def projectSettings = Seq(
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213, Scala212, Scala3),
    Test / testOptions ++= {
      List(Tests.Argument(MUnitFramework, "+l"))
    },
    ThisBuild / scalafixDependencies += OrganizeImports,
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    libraryDependencies ++= Seq(
      Cats.core,
      Cats.effect,
      Munit.core % Test,
      Munit.catsEffect % Test,
      Munit.scalacheck % Test,
      Munit.scalacheckEffect % Test
    ),
    moduleName := "kinesis4cats-" + name.value
  ) ++ Seq(
    addCommandAlias("cpl", ";Test / compile"),
    addCommandAlias(
      "fix",
      ";Compile / scalafix;Test / scalafix"
    ),
    addCommandAlias(
      "fmt",
      ";Compile / scalafmt;Test / scalafmt"
    ),
    addCommandAlias(
      "pretty",
      ";fix;fmt"
    ),
    addCommandAlias(
      "cov",
      ";clean;coverage;test;coverageReport;coverageOff"
    )
  ).flatten
}
