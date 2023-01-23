import sbt._

object LibraryDependencies {
  val OrganizeImports =
    "com.github.liancheng" %% "organize-imports" % "0.6.0"
  val Logback = "ch.qos.logback" % "logback-classic" % "1.4.5"
  val CatsRetry = "com.github.cb372" %% "cats-retry" % "3.1.0"

  object Log4Cats {
    val log4CatsVersion = "2.5.0"
    val slf4j = "org.typelevel" %% "log4cats-slf4j" % log4CatsVersion
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
    val sdkVersion = "2.19.17"
    val utils = "software.amazon.awssdk" % "utils" % sdkVersion
    val kinesis = "software.amazon.awssdk" % "kinesis" % sdkVersion
    val kpl = "com.amazonaws" % "amazon-kinesis-producer" % "0.14.13"
    val kcl = "software.amazon.kinesis" % "amazon-kinesis-client" % "2.4.4"
  }

  object Cats {
    val catsVersion = "2.9.0"
    val catsEffectVersion = "3.4.5"
    val core = "org.typelevel" %% "cats-core" % catsVersion
    val effect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  }

  object Circe {
    val circeVersion = "0.14.3"
    val core = "io.circe" %% "circe-core" % circeVersion
    val parser = "io.circe" %% "circe-parser" % circeVersion
  }
}
