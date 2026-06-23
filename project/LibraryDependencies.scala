import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object LibraryDependencies {
  val Logback = "ch.qos.logback" % "logback-classic" % "1.5.35"
  val JavaXMLBind = "javax.xml.bind" % "jaxb-api" % "2.3.1"
  val Scalacheck = Def.setting("org.scalacheck" %%% "scalacheck" % "1.19.0")

  object ScalaJS {
    val javaTime =
      Def.setting("io.github.cquiroz" %%% "scala-java-time" % "2.6.0")
  }

  object FS2 {
    val fs2Version = "3.13.0"
    val core = Def.setting("co.fs2" %%% "fs2-core" % fs2Version)
    val io = Def.setting("co.fs2" %%% "fs2-io" % fs2Version)
    val reactiveStreams = "co.fs2" %% "fs2-reactive-streams" % fs2Version
  }

  object Log4Cats {
    val log4CatsVersion = "2.8.0"
    val core =
      Def.setting("org.typelevel" %%% "log4cats-core" % log4CatsVersion)
    val slf4j = "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion
    val noop =
      Def.setting("org.typelevel" %%% "log4cats-noop" % log4CatsVersion)
  }

  object Munit {
    val core = Def.setting("org.scalameta" %%% "munit" % "1.3.3")
    val scalacheck =
      Def.setting("org.scalameta" %%% "munit-scalacheck" % "1.3.0")
    val catsEffect =
      Def.setting("org.typelevel" %%% "munit-cats-effect" % "2.2.0")
    val scalacheckEffect =
      Def.setting("org.typelevel" %%% "scalacheck-effect-munit" % "2.1.0")

  }

  object Aws {

    object V2 {
      val awssdkVersion =
        "2.46.16" // Should be the same as the latest version in the KCL or KPL
      val kinesis = "software.amazon.awssdk" % "kinesis" % awssdkVersion
      val dynamo = "software.amazon.awssdk" % "dynamodb" % awssdkVersion
      val cloudwatch = "software.amazon.awssdk" % "cloudwatch" % awssdkVersion
      val auth = "software.amazon.awssdk" % "auth" % awssdkVersion
      val httpAuthAws =
        "software.amazon.awssdk" % "http-auth-aws" % awssdkVersion
    }

    object Aggregation {
      val aggregator =
        "com.github.awslabs.kinesis-aggregation" % "amazon-kinesis-aggregator" % "2.0.3-agg"
      val deaggregator =
        "com.github.awslabs.kinesis-aggregation" % "amazon-kinesis-aggregator" % "2.0.3-deagg"
    }

    val kpl = "software.amazon.kinesis" % "amazon-kinesis-producer" % "1.0.7"
    val kcl = "software.amazon.kinesis" % "amazon-kinesis-client" % "3.4.3"
  }

  object Cats {
    val catsVersion = "2.13.0"
    val catsEffectVersion = "3.7.0"
    val core = Def.setting("org.typelevel" %%% "cats-core" % catsVersion)
    val effect =
      Def.setting("org.typelevel" %%% "cats-effect" % catsEffectVersion)
  }

  object Circe {
    val circeVersion = "0.14.15"
    val core = Def.setting("io.circe" %%% "circe-core" % circeVersion)
    val parser = Def.setting("io.circe" %%% "circe-parser" % circeVersion)
    val scodec = Def.setting("io.circe" %%% "circe-scodec" % circeVersion)
  }

  object Ciris {
    val cirisVersion = "3.15.0"
    val core = Def.setting("is.cir" %%% "ciris" % cirisVersion)
  }

  object Feral {
    val feralVersion = "0.3.1"
    val lambda = Def.setting("org.typelevel" %%% "feral-lambda" % feralVersion)
  }

  object Http4s {
    val http4sVersion = "0.23.34"
    val emberServer =
      Def.setting("org.http4s" %%% "http4s-ember-server" % http4sVersion)
    val emberClient =
      Def.setting("org.http4s" %%% "http4s-ember-client" % http4sVersion)
    val blazeClient =
      Def.setting("org.http4s" %%% "http4s-blaze-client" % "0.23.17")
    val circe = Def.setting("org.http4s" %%% "http4s-circe" % http4sVersion)
  }

  object Smithy {
    def rulesEngine(version: String) =
      "software.amazon.smithy" % "smithy-rules-engine" % version
    def build(version: String) =
      "software.amazon.smithy" % "smithy-build" % version
    // https://github.com/aws/api-models-aws
    val kinesis = "software.amazon.api.models" % "kinesis" % "1.0.8"
    val cloudwatch = "software.amazon.api.models" % "cloudwatch" % "1.0.12"
  }

  object Otel4s {
    val otel4sVersion = "1.0.1"
    val otel4sSdkVersion = "0.19.0"
    val coreMetrics =
      Def.setting("org.typelevel" %%% "otel4s-core-metrics" % otel4sVersion)
    val sdkMetricsTestkit =
      Def.setting(
        "org.typelevel" %%% "otel4s-sdk-metrics-testkit" % otel4sSdkVersion
      )
    // JVM-only OTel Java bridge (kinesis-client-opentelemetry)
    val otelJava =
      "org.typelevel" %% "otel4s-oteljava" % otel4sVersion
    // Pure-Scala otel4s SDK (smithy4s-client-opentelemetry)
    val sdkMetrics =
      Def.setting("org.typelevel" %%% "otel4s-sdk-metrics" % otel4sSdkVersion)
    val sdkExporterMetrics =
      Def.setting(
        "org.typelevel" %%% "otel4s-sdk-exporter-metrics" % otel4sSdkVersion
      )
  }

  object OtelJavaSdk {
    // OTLP/HTTP exporter for the OTel Java SDK. Pulls opentelemetry-sdk-metrics
    // and the OkHttp sender transitively. Pin to the version otel4s-oteljava
    // 1.0.0 resolves (verify with `sbt evicted`; bump to match if newer).
    val otlpExporterVersion = "1.63.0"
    val otlpExporter =
      "io.opentelemetry" % "opentelemetry-exporter-otlp" % otlpExporterVersion
  }

  object OkHttp {
    val mockWebServer =
      "com.squareup.okhttp3" % "mockwebserver" % "5.3.2" % "test"
  }
}
