import LibraryDependencies._

lazy val compat = project

lazy val kcl = project
  .settings(
    description := "Cats tooling for the Kinesis Client Library (KCL)",
    libraryDependencies ++= Seq(
      Aws.kcl,
      Log4Cats.slf4j,
      Circe.core,
      Circe.parser,
      CatsRetry
    )
  )
  .dependsOn(compat)

lazy val root = tlCrossRootProject.aggregate(compat, kcl)
