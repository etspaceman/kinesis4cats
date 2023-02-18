import sbt._

object LibraryDependencies {
  val Logback = "ch.qos.logback" % "logback-classic" % "1.4.5"
  val CatsRetry = "com.github.cb372" %% "cats-retry" % "3.1.0"
  val JavaXMLBind = "javax.xml.bind" % "jaxb-api" % "2.3.1"
  val Scalacheck = "org.scalacheck" %% "scalacheck" % "1.17.0"

  object Scala {
    val java8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2"
  }

  object FS2 {
    val fs2Version = "3.6.1"
    val core = "co.fs2" %% "fs2-core" % fs2Version
    val reactiveStreams = "co.fs2" %% "fs2-reactive-streams" % fs2Version
  }

  object Log4Cats {
    val log4CatsVersion = "2.5.0"
    val core = "org.typelevel" %% "log4cats-core" % log4CatsVersion
    val slf4j = "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion
    val noop = "org.typelevel" %% "log4cats-noop" % log4CatsVersion
  }

  object Munit {
    val munitVersion = "0.7.29"
    val core = "org.scalameta" %% "munit" % munitVersion
    val scalacheck = "org.scalameta" %% "munit-scalacheck" % munitVersion
    val catsEffect = "org.typelevel" %% "munit-cats-effect-3" % "1.0.7"
    val scalacheckEffect =
      "org.typelevel" %% "scalacheck-effect-munit" % "1.0.4"
  }

  object Aws {

    object V1 {
      val awsVersion =
        "1.12.296" // Should be the same as the version in the KPL
      val kinesis = "com.amazonaws" % "aws-java-sdk-kinesis" % awsVersion
      val dynamo = "com.amazonaws" % "aws-java-sdk-dynamodb" % awsVersion
      val cloudwatch = "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsVersion
    }

    object V2 {
      val awssdkVersion =
        "2.19.2" // Should be the same as the version in the KCL
      val kinesis = "software.amazon.awssdk" % "kinesis" % awssdkVersion
      val dynamo = "software.amazon.awssdk" % "dynamodb" % awssdkVersion
      val cloudwatch = "software.amazon.awssdk" % "cloudwatch" % awssdkVersion
    }

    // See https://github.com/etspaceman/kinesis-mock/pull/407, upgrade when available in localstack
    val kpl = "com.amazonaws" % "amazon-kinesis-producer" % "0.14.13"
    val kcl = "software.amazon.kinesis" % "amazon-kinesis-client" % "2.4.5"
  }

  object Cats {
    val catsVersion = "2.9.0"
    val catsEffectVersion = "3.4.7"
    val core = "org.typelevel" %% "cats-core" % catsVersion
    val effect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  }

  object Circe {
    val circeVersion = "0.14.3"
    val core = "io.circe" %% "circe-core" % circeVersion
    val parser = "io.circe" %% "circe-parser" % circeVersion
  }

  object Ciris {
    val cirisVersion = "3.1.0"
    val core = "is.cir" %% "ciris" % cirisVersion
  }

  object Http4s {
    val http4sVersion = "0.23.18-98-66d0795-SNAPSHOT"
    val emberServer = "org.http4s" %% "http4s-ember-server" % http4sVersion
    val emberClient = "org.http4s" %% "http4s-ember-client" % http4sVersion
    val blazeClient = "org.http4s" %% "http4s-blaze-client" % "0.23.13"
    val circe = "org.http4s" %% "http4s-circe" % http4sVersion
  }

  object Smithy {
    def rulesEngine(version: String) =
      "software.amazon.smithy" % "smithy-rules-engine" % version
    def build(version: String) =
      "software.amazon.smithy" % "smithy-build" % version
  }

  object Smithy4s {
    def core(version: String) =
      "com.disneystreaming.smithy4s" %% "smithy4s-core" % version
    def http4s(version: String) =
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % version
    def http4sSwagger(version: String) =
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % version
    def http4sAws(version: String) =
      "com.disneystreaming.smithy4s" %% "smithy4s-aws-http4s" % version
    val kinesis =
      "com.disneystreaming.smithy" % "aws-kinesis-spec" % "2023.02.10"
  }
}
