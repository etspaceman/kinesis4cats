import LibraryDependencies._

lazy val kcl = project.settings(
  description := "Cats tooling for the Kinesis Client Library (KCL)",
  libraryDependencies ++= Seq(
    Aws.kcl,
    Log4Cats.slf4j,
    CatsRetry
  )
)

lazy val root = tlCrossRootProject.aggregate(kcl)
