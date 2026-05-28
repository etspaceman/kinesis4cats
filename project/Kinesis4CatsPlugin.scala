import java.time.Year

import LibraryDependencies._
import org.scalafmt.sbt.ScalafmtPlugin
import org.typelevel.sbt._
import org.typelevel.sbt.gha._
import org.typelevel.sbt.mergify._
import sbt.Keys._
import sbt._
import sbtcrossproject.CrossPlugin.autoImport._
import sbtcrossproject.CrossProject

object Kinesis4CatsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires: Plugins = TypelevelPlugin

  def mkCommand(commands: List[String]): String =
    commands.mkString("; ", "; ", "")

  val autoImport: Kinesis4CatsPluginKeys.type = Kinesis4CatsPluginKeys

  import scala.scalanative.sbtplugin.ScalaNativePlugin.autoImport._

  import DockerComposePlugin.autoImport._
  import DockerImagePlugin.autoImport._
  import GenerativePlugin.autoImport._
  import MergifyPlugin.autoImport._
  import TypelevelCiPlugin.autoImport._
  import TypelevelGitHubPlugin.autoImport._
  import TypelevelKernelPlugin.autoImport._
  import TypelevelSettingsPlugin.autoImport._
  import TypelevelSitePlugin.autoImport._
  import TypelevelSonatypePlugin.autoImport._
  import TypelevelVersioningPlugin.autoImport._
  import autoImport._
  import com.armanbilge.sbt.ScalaNativeBrewedGithubActionsPlugin.autoImport._
  import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
  import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
  import sbtassembly.AssemblyPlugin.autoImport._
  import scalafix.sbt.ScalafixPlugin.autoImport._

  private val onlyJvm = Def.setting {
    val java = githubWorkflowJavaVersions.value.head
    val os = githubWorkflowOSes.value.head
    s"matrix.java == '${java.render}' && matrix.os == '${os}' && endsWith(matrix.project, 'JVM')"
  }

  private val onlyJs = Def.setting {
    val java = githubWorkflowJavaVersions.value.head
    val os = githubWorkflowOSes.value.head
    s"matrix.java == '${java.render}' && matrix.os == '${os}' && endsWith(matrix.project, 'JS')"
  }

  private val onlyNative = Def.setting {
    val java = githubWorkflowJavaVersions.value.head
    val os = githubWorkflowOSes.value.head
    s"matrix.java == '${java.render}' && matrix.os == '${os}' && endsWith(matrix.project, 'Native')"
  }

  private val onlyJvmScala3 = Def.setting {
    onlyJvm.value + s" && matrix.scala == '$Scala3'"
  }

  override def buildSettings = Seq(
    tlBaseVersion := "0.4",
    organization := "io.github.etspaceman",
    organizationName := "etspaceman",
    startYear := Some(2023),
    licenses := Seq(License.Apache2),
    developers := List(tlGitHubDev("etspaceman", "Eric Meisel")),
    crossScalaVersions := allScalaVersions,
    scalaVersion := Scala3,
    runIntegrationTests := false,
    tlCiHeaderCheck := true,
    tlCiScalafmtCheck := true,
    tlCiScalafixCheck := true,
    resolvers += "s01 snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/",
    resolvers += "jitpack" at "https://jitpack.io",
    githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17")),
    githubWorkflowBuildPreamble ++= nativeBrewInstallWorkflowSteps.value,
    githubWorkflowBuildMatrixFailFast := Some(false),
    githubWorkflowBuild ++= {
      val jvm = onlyJvm.value
      val jvmScala3 = onlyJvmScala3.value
      List(
        WorkflowStep.Sbt(
          List("rootJVMPlain3/Test/compile"),
          name = Some("Compile (JVM plain projects)"),
          cond = Some(jvmScala3)
        ),
        WorkflowStep.Sbt(
          List("dockerComposeUp"),
          name = Some("Docker Compose Up"),
          cond = Some(jvm),
          env = Map(
            "GITHUB_API_TOKEN" -> "${{ secrets.GITHUB_TOKEN }}",
            "LOCALSTACK_AUTH_TOKEN" -> "${{ secrets.LOCALSTACK_AUTH_TOKEN }}"
          )
        ),
        WorkflowStep.Sbt(
          List("itTest"),
          name = Some("Integration Tests"),
          cond = Some(jvm)
        ),
        WorkflowStep.Sbt(
          List("dockerComposePs", "dockerComposeLogs"),
          name = Some("Print docker logs and container listing"),
          cond = Some("${{ failure() }}")
        ),
        WorkflowStep.Sbt(
          List("dockerComposeDown"),
          name = Some("Remove docker containers"),
          cond = Some(jvm)
        )
      )
    },
    mergifyStewardConfig := Some(
      MergifyStewardConfig(
        action = MergifyAction.Merge(method = Some("squash")),
        author = "etspaceman-scala-steward-app[bot]"
      )
    )
  )

  override def projectSettings = Seq(
    Test / testOptions := Seq(
      Tests.Argument(TestFrameworks.MUnit, "+l"),
      Tests.Argument(
        TestFrameworks.MUnit,
        if ((ThisBuild / runIntegrationTests).value)
          "--include-tags=integration"
        else "--exclude-tags=integration"
      )
    ),
    scalacOptions ++= {
      val base = Seq("-language:implicitConversions")
      if (tlIsScala3.value) base
      else base :+ "-Wconf:src=src_managed/.*:silent"
    },
    scalacOptions := scalacOptions.value.distinct,
    ThisBuild / semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    libraryDependencies ++= Seq(
      Cats.core.value,
      Cats.effect.value,
      Log4Cats.core.value,
      FS2.core.value,
      FS2.io.value
    ) ++ testDependencies.value.map(_ % Test),
    libraryDependencySchemes ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
      "org.scala-native" % "test-interface_native0.5_3" % VersionScheme.Always,
      "org.scala-native" % "test-interface_native0.5_2.13" % VersionScheme.Always
    ),
    moduleName := "kinesis4cats-" + name.value,
    headerLicense := Some(
      HeaderLicense.ALv2(
        s"${startYear.value.get}-${Year.now.getValue}",
        organizationName.value
      )
    ),
    Compile / doc / sources := {
      if (scalaVersion.value.startsWith("3.")) Nil
      else (Compile / doc / sources).value
    },
    assembly / test := {},
    Test / parallelExecution := false,
    tlJdkRelease := Some(8)
  ) ++ Seq(
    addCommandAlias(
      "cpl",
      ";Test / compile"
    ),
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
      ";githubWorkflowGenerate;headerCreateAll;fix;fmt"
    ),
    addCommandAlias(
      "prettyCheck",
      ";headerCheckAll;fixCheck;fmtCheck"
    ),
    addCommandAlias(
      "cov",
      ";clean;coverage;test;coverageReport;coverageOff"
    ),
    addCommandAlias(
      "itTest",
      ";set ThisBuild/runIntegrationTests := true;test;set ThisBuild/runIntegrationTests := false"
    )
  ).flatten

  override def globalSettings: Seq[Setting[_]] = Seq(
    concurrentRestrictions += Tags.limit(NativeTags.Link, 1)
  )
}

object Kinesis4CatsPluginKeys {
  val Scala213 = "2.13.18"
  val Scala3 = "3.3.7"

  val allScalaVersions = List(Scala213, Scala3)

  val runIntegrationTests = settingKey[Boolean](
    "When true, test tasks include integration-tagged tests; when false (default), they exclude them."
  )

  import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
  import scalafix.sbt.ScalafixPlugin.autoImport._

  val testDependencies = Def.setting(
    List(
      Munit.core.value,
      Munit.catsEffect.value,
      Munit.scalacheck.value,
      Scalacheck.value
    )
  )

  final implicit class Kinesis4CatsCrossProjectOps(private val p: CrossProject)
      extends AnyVal {
    def forkTests = p.jvmSettings(Test / fork := true)
  }
}
