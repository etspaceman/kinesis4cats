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

  private val onlyScalaJsCond = Def.setting {
    primaryJavaOSCond.value + s" && startsWith(matrix.project, 'root-js')"
  }

  private val onlyNativeCond = Def.setting {
    primaryJavaOSCond.value + s" && startsWith(matrix.project, 'root-native')"
  }

  private val onlyFailures = Def.setting {
    "${{ failure() }}"
  }

  override def buildSettings = Seq(
    tlBaseVersion := "0.0",
    organization := "io.github.etspaceman",
    organizationName := "etspaceman",
    startYear := Some(2023),
    licenses := Seq(License.Apache2),
    developers := List(tlGitHubDev("etspaceman", "Eric Meisel")),
    crossScalaVersions := Seq(Scala213),
    scalaVersion := Scala213,
    tlCiMimaBinaryIssueCheck := tlBaseVersion.value != "0.0",
    tlSonatypeUseLegacyHost := true,
    resolvers += "s01 snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/",
    resolvers += "jitpack" at "https://jitpack.io",
    githubWorkflowBuildPreamble ++= nativeBrewInstallWorkflowSteps.value,
    githubWorkflowBuildMatrixFailFast := Some(false),
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
        WorkflowStep.Use(
          UseRef.Public("nick-fields", "retry", "v2"),
          name = Some("Docker Compose Up"),
          cond = Some(primaryJavaOSCond.value),
          params = Map(
            "timeout_minutes" -> "15",
            "max_attempts" -> "3",
            "command" -> "sbt 'project ${{ matrix.project }}' dockerComposeUp",
            "retry_on" -> "error",
            "on_retry_command" -> "sbt 'project ${{ matrix.project }}' dockerComposeDown"
          ),
          env = Map("GITHUB_API_TOKEN" -> "${{ secrets.GITHUB_TOKEN }}")
        ),
        WorkflowStep.Sbt(
          List("Test/fastLinkJS"),
          name = Some("Link JS"),
          cond = Some(onlyScalaJsCond.value)
        ),
        WorkflowStep.Use(
          UseRef.Public("nick-fields", "retry", "v2"),
          name = Some("Link Native"),
          cond = Some(onlyNativeCond.value),
          params = Map(
            "timeout_minutes" -> "25",
            "max_attempts" -> "3",
            "command" -> "sbt 'project ${{ matrix.project }}' Test/nativeLink",
            "retry_on" -> "error"
          ),
          env = Map("GITHUB_API_TOKEN" -> "${{ secrets.GITHUB_TOKEN }}")
        ),
        WorkflowStep.Sbt(
          List(
            "test"
          ),
          name = Some("Test"),
          cond = Some(primaryJavaOSCond.value)
        ),
        WorkflowStep.Sbt(
          List(
            "dockerComposePs",
            "dockerComposeLogs"
          ),
          name = Some("Print docker logs and container listing"),
          cond = Some(onlyFailures.value)
        ),
        WorkflowStep.Sbt(
          List(
            "dockerComposeDown"
          ),
          name = Some("Remove docker containers"),
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
    githubWorkflowBuildSbtStepPreamble := Seq(
      s"project $${{ matrix.project }}"
    ),
    githubWorkflowArtifactDownloadExtraKeys += "project",
    tlCiScalafixCheck := true,
    mergifyStewardConfig := Some(
      MergifyStewardConfig(
        action = MergifyAction.Merge(method = Some("squash")),
        author = "etspaceman-scala-steward-app[bot]"
      )
    )
  )

  override def projectSettings = Seq(
    Test / testOptions ++= {
      List(Tests.Argument(TestFrameworks.MUnit, "+l"))
    },
    // Workaround for https://github.com/typelevel/sbt-typelevel/issues/464
    scalacOptions ++= {
      if (tlIsScala3.value)
        Seq(
          "-language:implicitConversions",
          "-Ykind-projector"
        )
      else
        Seq(
          "-Wconf:src=src_managed/.*:silent"
        )
    },
    scalacOptions -= "-Ykind-projector:underscores",
    ThisBuild / semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    libraryDependencies ++= Seq(
      Cats.core.value,
      Cats.effect.value,
      Log4Cats.core.value,
      FS2.core.value
    ) ++ testDependencies.value.map(_ % Test),
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always,
    moduleName := "kinesis4cats-" + name.value,
    headerLicense := Some(
      HeaderLicense.ALv2(s"${startYear.value.get}-2023", organizationName.value)
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
    )
  ).flatten

  override def globalSettings: Seq[Setting[_]] = Seq(
    concurrentRestrictions += Tags.limit(NativeTags.Link, 1),
    tlCommandAliases ++= Map(
      "prePR" -> List(
        "reload",
        "project /",
        "clean",
        "cpl",
        "pretty",
        "set ThisBuild / tlFatalWarnings := tlFatalWarningsInCi.value",
        "doc",
        "session clear"
      ),
      "tlRelease" -> List(
        "reload",
        "project /",
        // Uncomment this when 0.1.x
        // "+mimaReportBinaryIssues",
        "+publish",
        "tlSonatypeBundleReleaseIfRelevant"
      )
    )
  )
}

object Kinesis4CatsPluginKeys {
  val Scala212 = "2.12.19"
  val Scala213 = "2.13.14"
  val Scala3 = "3.3.3"

  val allScalaVersions = List(Scala213, Scala3, Scala212)
  val last2ScalaVersions = List(Scala213, Scala3)

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

  final implicit class Kinesi4CatsProjectMatrixOps(private val p: ProjectMatrix)
      extends AnyVal {
    def forkTests = p.settings(Test / fork := true)
  }
}
