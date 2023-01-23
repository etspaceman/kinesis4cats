import sbt._
import sbt.Keys._

import org.typelevel.sbt._
import org.typelevel.sbt.gha._
import LibraryDependencies._

object Kinesis4CatsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires: Plugins = TypelevelPlugin

  def mkCommand(commands: List[String]): String =
    commands.mkString("; ", "; ", "")

  import TypelevelVersioningPlugin.autoImport._
  import TypelevelGitHubPlugin.autoImport._
  import TypelevelKernelPlugin.autoImport._
  import TypelevelCiPlugin.autoImport._
  import GenerativePlugin.autoImport._
  import scalafix.sbt.ScalafixPlugin.autoImport._
  import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
  import scoverage.ScoverageSbtPlugin.autoImport._

  val Scala212 = "2.12.17"
  val Scala213 = "2.13.10"
  val Scala3 = "3.2.1"

  val MUnitFramework = new TestFramework("munit.Framework")
  private val primaryJavaOSCond = Def.setting {
    val java = githubWorkflowJavaVersions.value.head
    val os = githubWorkflowOSes.value.head
    s"matrix.java == '${java.render}' && matrix.os == '${os}'"
  }

  private val noScala3Cond = Def.setting {
    primaryJavaOSCond.value + s" && matrix.scala != '$Scala3'"
  }

  private val onlyScala3Cond = Def.setting {
    primaryJavaOSCond.value + s" && matrix.scala == '$Scala3'"
  }

  override def buildSettings = Seq(
    tlBaseVersion := "0.0",
    organization := "etspaceman",
    organizationName := "etspaceman",
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213, Scala212, Scala3),
    githubWorkflowScalaVersions := crossScalaVersions.value,
    startYear := Some(2023),
    licenses := Seq(License.Apache2),
    developers := List(
      // your GitHub handle and name
      tlGitHubDev("etspaceman", "Eric Meisel")
    ),
    coverageScalacPluginVersion := "2.0.7",
    githubWorkflowBuild := {
      val style = (tlCiHeaderCheck.value, tlCiScalafmtCheck.value) match {
        case (true, true) => // headers + formatting
          List(
            WorkflowStep.Sbt(
              List(
                "headerCheckAll",
                "fmtCheck"
              ),
              name = Some("Check headers and formatting"),
              cond = Some(primaryJavaOSCond.value)
            )
          )
        case (true, false) => // headers
          List(
            WorkflowStep.Sbt(
              List("headerCheckAll"),
              name = Some("Check headers"),
              cond = Some(primaryJavaOSCond.value)
            )
          )
        case (false, true) => // formatting
          List(
            WorkflowStep.Sbt(
              List("fmtCheck"),
              name = Some("Check formatting"),
              cond = Some(primaryJavaOSCond.value)
            )
          )
        case (false, false) => Nil // nada
      }

      val testWithCoverage = List(
        WorkflowStep.Sbt(
          List("cov"),
          name = Some("Test with coverage"),
          cond = Some(noScala3Cond.value)
        )
      )

      val test = List(
        WorkflowStep.Sbt(
          List("test"),
          name = Some("Test"),
          cond = Some(onlyScala3Cond.value)
        )
      )

      val scalafix =
        if (tlCiScalafixCheck.value)
          List(
            WorkflowStep.Sbt(
              List("fixCheck"),
              name = Some("Check scalafix lints"),
              cond = Some(noScala3Cond.value)
            )
          )
        else Nil

      val mima =
        if (tlCiMimaBinaryIssueCheck.value)
          List(
            WorkflowStep.Sbt(
              List("mimaReportBinaryIssues"),
              name = Some("Check binary compatibility"),
              cond = Some(primaryJavaOSCond.value)
            )
          )
        else Nil

      val doc =
        if (tlCiDocCheck.value)
          List(
            WorkflowStep.Sbt(
              List("doc"),
              name = Some("Generate API documentation"),
              cond = Some(primaryJavaOSCond.value)
            )
          )
        else Nil

      val cov =
        List(
          WorkflowStep.Use(
            UseRef.Public("codecov", "codecov-action", "v2"),
            name = Some("Upload coverage")
          )
        )

      style ++ testWithCoverage ++ test ++ scalafix ++ mima ++ doc ++ cov
    },
    githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17")),
    tlCiScalafixCheck := true
  ) ++ tlReplaceCommandAlias(
    "prePR",
    mkCommand(
      List(
        "reload",
        "project /",
        "clean",
        "githubWorkflowGenerate",
        "cpl",
        "+headerCreateAll",
        "pretty",
        "set ThisBuild / tlFatalWarnings := tlFatalWarningsInCi.value",
        "doc",
        "session clear"
      )
    )
  )

  override def projectSettings = Seq(
    Test / testOptions ++= {
      List(Tests.Argument(MUnitFramework, "+l"))
    },
    ThisBuild / scalafixDependencies += OrganizeImports,
    ThisBuild / semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    libraryDependencies ++= Seq(
      Cats.core,
      Cats.effect,
      CatsRetry,
      Munit.core % Test,
      Munit.catsEffect % Test,
      Munit.scalacheck % Test,
      Munit.scalacheckEffect % Test
    ),
    moduleName := "kinesis4cats-" + name.value,
    headerLicense := Some(
      HeaderLicense.ALv2(s"${startYear.value.get}-2023", organizationName.value)
    )
  ) ++ Seq(
    addCommandAlias("cpl", ";+Test / compile"),
    addCommandAlias(
      "fixCheck",
      ";Compile / scalafix --check;Test / scalafix --check"
    ),
    addCommandAlias(
      "fix",
      ";Compile / scalafix;Test / scalafix"
    ),
    addCommandAlias(
      "fmtCheck",
      ";Compile / scalafmtCheck;Test / scalafmtCheck;scalafmtSbtCheck"
    ),
    addCommandAlias(
      "fmt",
      ";Compile / scalafmt;Test / scalafmt;scalafmtSbt"
    ),
    addCommandAlias(
      "pretty",
      "fix;fmt"
    ),
    addCommandAlias(
      "prettyCheck",
      ";fixCheck;fmtCheck"
    ),
    addCommandAlias(
      "cov",
      ";clean;coverage;test;coverageReport;coverageOff"
    )
  ).flatten
}
