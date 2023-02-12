import LibraryDependencies._
import org.scalafmt.sbt.ScalafmtPlugin
import org.typelevel.sbt._
import org.typelevel.sbt.gha._
import org.typelevel.sbt.mergify._
import sbt.Keys._
import sbt._
import sbt.internal.ProjectMatrix

object Kinesis4CatsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires: Plugins = TypelevelPlugin

  def mkCommand(commands: List[String]): String =
    commands.mkString("; ", "; ", "")

  val autoImport: Kinesis4CatsPluginKeys.type = Kinesis4CatsPluginKeys
  import DockerComposePlugin.autoImport._
  import DockerImagePlugin.autoImport._
  import GenerativePlugin.autoImport._
  import MergifyPlugin.autoImport._
  import TypelevelCiPlugin.autoImport._
  import TypelevelGitHubPlugin.autoImport._
  import TypelevelKernelPlugin.autoImport._
  import TypelevelSitePlugin.autoImport._
  import TypelevelVersioningPlugin.autoImport._
  import autoImport._
  import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
  import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
  import sbtassembly.AssemblyPlugin.autoImport._
  import scalafix.sbt.ScalafixPlugin.autoImport._

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
    organization := "io.github.etspaceman",
    organizationName := "etspaceman",
    startYear := Some(2023),
    licenses := Seq(License.Apache2),
    developers := List(tlGitHubDev("etspaceman", "Eric Meisel")),
    crossScalaVersions := Seq(Scala212, Scala3, Scala213),
    scalaVersion := Scala213,
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

      val test = List(
        WorkflowStep.Sbt(
          List(
            "IT / dockerComposeUp",
            "test",
            "IT / test",
            "IT / dockerComposeDown",
            "FunctionalTest / dockerComposeUp",
            "FunctionalTest / test",
            "FunctionalTest / dockerComposeDown"
          ),
          name = Some("Test"),
          cond = Some(primaryJavaOSCond.value)
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

      style ++ test ++ scalafix ++ mima ++ doc
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
        "headerCreateAll",
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
    // Workaround for https://github.com/typelevel/sbt-typelevel/issues/464
    scalacOptions ++= {
      if (tlIsScala3.value)
        Seq(
          "-language:implicitConversions",
          "-Ykind-projector",
          "-source:3.0-migration"
        )
      else
        Seq("-language:_")
    },
    scalacOptions -= "-Ykind-projector:underscores",
    ThisBuild / semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    libraryDependencies ++= Seq(
      Cats.core,
      Cats.effect,
      CatsRetry,
      Scala.java8Compat,
      Log4Cats.core,
      Munit.core % Test,
      Munit.catsEffect % Test,
      Munit.scalacheck % Test,
      Munit.scalacheckEffect % Test,
      Logback % Test,
      Scalacheck % Test,
      FS2.core % Test,
      FS2.reactiveStreams % Test
    ),
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    moduleName := "kinesis4cats-" + name.value,
    headerLicense := Some(
      HeaderLicense.ALv2(s"${startYear.value.get}-2023", organizationName.value)
    ),
    Test / fork := true,
    Compile / doc / sources := {
      if (scalaVersion.value.startsWith("3.")) Nil
      else (Compile / doc / sources).value
    },
    assembly / test := {}
  ) ++ Seq(
    addCommandAlias(
      "cpl",
      ";Test / compile;IT / compile;FunctionalTest / compile"
    ),
    addCommandAlias(
      "fixCheck",
      ";Compile / scalafix --check;Test / scalafix --check;IT / scalafix --check;FunctionalTest / scalafix --check"
    ),
    addCommandAlias(
      "fix",
      ";Compile / scalafix;Test / scalafix;IT / scalafix;FunctionalTest / scalafix"
    ),
    addCommandAlias(
      "fmtCheck",
      ";Compile / scalafmtCheck;Test / scalafmtCheck;IT / scalafmtCheck;FunctionalTest / scalafmtCheck;scalafmtSbtCheck"
    ),
    addCommandAlias(
      "fmt",
      ";Compile / scalafmt;Test / scalafmt;IT / scalafmt;FunctionalTest / scalafmt;scalafmtSbt"
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
      ";clean;coverage;test;IT/test;coverageReport;coverageOff"
    )
  ).flatten
}

object Kinesis4CatsPluginKeys {
  val Scala212 = "2.12.17"
  val Scala213 = "2.13.10"
  val Scala3 = "3.2.2"

  val allScalaVersions = List(Scala213, Scala3, Scala212)
  val last2ScalaVersions = List(Scala213, Scala3)

  val MUnitFramework = new TestFramework("munit.Framework")

  import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
  import scalafix.sbt.ScalafixPlugin.autoImport._

  val IT = config("it").extend(Test)
  val FunctionalTest = config("fun").extend(Test)

  final implicit class Kinesi4CatsProjectMatrixOps(private val p: ProjectMatrix)
      extends AnyVal {
    def enableIntegrationTests = p
      .configs(IT)
      .settings(inConfig(IT) {
        ScalafmtPlugin.scalafmtConfigSettings ++
          scalafixConfigSettings(IT) ++
          BloopSettings.default ++
          Defaults.testSettings ++
          headerSettings(IT) ++
          Seq(
            parallelExecution := false,
            javaOptions += "-Dcom.amazonaws.sdk.disableCertChecking=true"
          )
      })

    def enableFunctionalTests = p
      .configs(FunctionalTest)
      .settings(inConfig(FunctionalTest) {
        ScalafmtPlugin.scalafmtConfigSettings ++
          scalafixConfigSettings(FunctionalTest) ++
          BloopSettings.default ++
          Defaults.testSettings ++
          headerSettings(FunctionalTest) ++
          Seq(
            parallelExecution := false,
            javaOptions += "-Dcom.amazonaws.sdk.disableCertChecking=true"
          )
      })
  }

  final implicit class Kinesis4CatsProjectOps(private val p: Project)
      extends AnyVal {
    def enableIntegrationTests = p
      .configs(IT)
      .settings(inConfig(IT) {
        ScalafmtPlugin.scalafmtConfigSettings ++
          scalafixConfigSettings(IT) ++
          BloopSettings.default ++
          Defaults.testSettings ++
          headerSettings(IT) ++
          Seq(
            parallelExecution := false,
            javaOptions += "-Dcom.amazonaws.sdk.disableCertChecking=true"
          )
      })

    def enableFunctionalTests = p
      .configs(FunctionalTest)
      .settings(inConfig(FunctionalTest) {
        ScalafmtPlugin.scalafmtConfigSettings ++
          scalafixConfigSettings(FunctionalTest) ++
          BloopSettings.default ++
          Defaults.testSettings ++
          headerSettings(FunctionalTest) ++
          Seq(
            parallelExecution := false,
            javaOptions += "-Dcom.amazonaws.sdk.disableCertChecking=true"
          )
      })
  }
}
