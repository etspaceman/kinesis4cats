import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object LibraryDependencies {
  val Logback = "ch.qos.logback" % "logback-classic" % "1.4.8"
  val JavaXMLBind = "javax.xml.bind" % "jaxb-api" % "2.3.1"
  val Scalacheck = Def.setting("org.scalacheck" %%% "scalacheck" % "1.17.0")
  val Epollcat = Def.setting("com.armanbilge" %%% "epollcat" % "0.1.5")

  object ScalaJS {
    val javaTime =
      Def.setting("io.github.cquiroz" %%% "scala-java-time" % "2.5.0")
  }

  object FS2 {
    val fs2Version = "3.7.0"
    val core = Def.setting("co.fs2" %%% "fs2-core" % fs2Version)
    val reactiveStreams = "co.fs2" %% "fs2-reactive-streams" % fs2Version
  }

  object Log4Cats {
    val log4CatsVersion = "2.6.0"
    val core =
      Def.setting("org.typelevel" %%% "log4cats-core" % log4CatsVersion)
    val slf4j = "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion
    val noop =
      Def.setting("org.typelevel" %%% "log4cats-noop" % log4CatsVersion)
  }

  object Munit {
    val munitVersion = "1.0.0-M8"
    val core = Def.setting("org.scalameta" %%% "munit" % munitVersion)
    val scalacheck =
      Def.setting("org.scalameta" %%% "munit-scalacheck" % munitVersion)
    val catsEffect =
      Def.setting("org.typelevel" %%% "munit-cats-effect" % "2.0.0-M3")
    val scalacheckEffect =
      Def.setting("org.typelevel" %%% "scalacheck-effect-munit" % "2.0.0-M2")

  }

  object Aws {

    object V1 {
      val awsVersion =
        "1.12.382" // Should be the same as the version in the KPL
      val kinesis = "com.amazonaws" % "aws-java-sdk-kinesis" % awsVersion
      val dynamo = "com.amazonaws" % "aws-java-sdk-dynamodb" % awsVersion
      val cloudwatch = "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsVersion
    }

    object V2 {
      val awssdkVersion =
        "2.20.43" // Should be the same as the version in the KCL
      val kinesis = "software.amazon.awssdk" % "kinesis" % awssdkVersion
      val dynamo = "software.amazon.awssdk" % "dynamodb" % awssdkVersion
      val cloudwatch = "software.amazon.awssdk" % "cloudwatch" % awssdkVersion
    }

    object Aggregation {
      val aggregator =
        "com.github.awslabs.kinesis-aggregation" % "amazon-kinesis-aggregator" % "2.0.3-agg"
      val deaggregator =
        "com.github.awslabs.kinesis-aggregation" % "amazon-kinesis-aggregator" % "2.0.3-deagg"
    }

    val kpl = "com.amazonaws" % "amazon-kinesis-producer" % "0.15.7"
    val kcl = "software.amazon.kinesis" % "amazon-kinesis-client" % "2.5.1"
  }

  object Cats {
    val catsVersion = "2.9.0"
    val catsEffectVersion = "3.5.1"
    val core = Def.setting("org.typelevel" %%% "cats-core" % catsVersion)
    val effect =
      Def.setting("org.typelevel" %%% "cats-effect" % catsEffectVersion)
  }

  object Circe {
    val circeVersion = "0.14.5"
    val core = Def.setting("io.circe" %%% "circe-core" % circeVersion)
    val parser = Def.setting("io.circe" %%% "circe-parser" % circeVersion)
    val scodec = Def.setting("io.circe" %%% "circe-scodec" % circeVersion)
  }

  object Ciris {
    val cirisVersion = "3.2.0"
    val core = Def.setting("is.cir" %%% "ciris" % cirisVersion)
  }

  object Http4s {
    val http4sVersion = "0.23.22"
    val emberServer =
      Def.setting("org.http4s" %%% "http4s-ember-server" % http4sVersion)
    val emberClient =
      Def.setting("org.http4s" %%% "http4s-ember-client" % http4sVersion)
    val blazeClient =
      Def.setting("org.http4s" %%% "http4s-blaze-client" % "0.23.15")
    val circe = Def.setting("org.http4s" %%% "http4s-circe" % http4sVersion)
  }

  object Smithy {
    def rulesEngine(version: String) =
      "software.amazon.smithy" % "smithy-rules-engine" % version
    def build(version: String) =
      "software.amazon.smithy" % "smithy-build" % version
  }

  object Smithy4s {
    val smithySpecVersion = "2023.02.10"

    val kinesis =
      "com.disneystreaming.smithy" % "aws-kinesis-spec" % smithySpecVersion
    val dynamo =
      "com.disneystreaming.smithy" % "aws-dynamodb-spec" % smithySpecVersion
    val cloudwatch =
      "com.disneystreaming.smithy" % "aws-cloudwatch-spec" % smithySpecVersion
  }
}
