import LibraryDependencies._

lazy val compat = project.settings(
  description := "Code to maintain compatability across major scala versions"
)

lazy val shared = project
  .settings(
    description := "Common shared utilities"
  )
  .dependsOn(compat)

lazy val `shared-circe` = project
  .settings(
    description := "Common shared utilities for Circe",
    libraryDependencies ++= Seq(
      Circe.core,
      Circe.parser
    )
  )
  .dependsOn(shared)

lazy val kcl = project
  .settings(
    description := "Cats tooling for the Kinesis Client Library (KCL)",
    libraryDependencies ++= Seq(
      Aws.kcl,
      Log4Cats.slf4j
    )
  )
  .dependsOn(shared)

lazy val `kcl-logging-circe` = project
  .settings(
    description := "JSON structured ogging instances, via Circe"
  )
  .dependsOn(kcl, `shared-circe`)

lazy val root =
  tlCrossRootProject.aggregate(
    compat,
    shared,
    `shared-circe`,
    kcl,
    `kcl-logging-circe`
  )
