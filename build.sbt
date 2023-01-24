import LibraryDependencies._

lazy val compat = project
  .settings(
    description := "Code to maintain compatability across major scala versions"
  )
  .enableIntegrationTests

lazy val shared = project
  .settings(
    description := "Common shared utilities"
  )
  .enableIntegrationTests
  .dependsOn(compat)

lazy val `shared-circe` = project
  .settings(
    description := "Common shared utilities for Circe",
    libraryDependencies ++= Seq(
      Circe.core,
      Circe.parser
    )
  )
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `shared-ciris` = project
  .settings(
    description := "Common shared utilities for Ciris",
    libraryDependencies ++= Seq(Ciris.core)
  )
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `localstack-test-kit-common` = project
  .settings(
    description := "Common utilities for the localstack test-kits"
  )
  .enableIntegrationTests
  .dependsOn(shared, `shared-ciris`)

lazy val `localstack-aws-v2-test-kit` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the V2 AWS SDK",
    libraryDependencies ++= Seq(
      Aws.V2.kinesis,
      Aws.V2.dynamo,
      Aws.V2.cloudwatch
    )
  )
  .enableIntegrationTests
  .dependsOn(`localstack-test-kit-common`)

lazy val `localstack-aws-v1-test-kit` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the V1 AWS SDK",
    libraryDependencies ++= Seq(
      Aws.V1.kinesis,
      Aws.V1.dynamo,
      Aws.V1.cloudwatch
    )
  )
  .enableIntegrationTests
  .dependsOn(`localstack-test-kit-common`)

lazy val kcl = project
  .settings(
    description := "Cats tooling for the Kinesis Client Library (KCL)",
    libraryDependencies ++= Seq(
      Aws.kcl,
      Log4Cats.slf4j
    )
  )
  .enableIntegrationTests
  .dependsOn(shared, `localstack-aws-v2-test-kit` % IT)

lazy val `kcl-logging-circe` = project
  .settings(
    description := "JSON structured logging instances for the KCL, via Circe"
  )
  .enableIntegrationTests
  .dependsOn(kcl, `shared-circe`, `localstack-aws-v2-test-kit` % IT)

lazy val kpl = project
  .settings(
    description := "Cats tooling for the Kinesis Producer Library (KPL)",
    libraryDependencies ++= Seq(
      Aws.kpl,
      Log4Cats.slf4j,
      JavaXMLBind
    )
  )
  .enableIntegrationTests
  .dependsOn(shared, `localstack-aws-v1-test-kit` % IT)

lazy val `kpl-logging-circe` = project
  .settings(
    description := "JSON structured logging instances for the KPL, via Circe"
  )
  .enableIntegrationTests
  .dependsOn(kpl, `shared-circe`, `localstack-aws-v1-test-kit` % IT)

lazy val root =
  tlCrossRootProject
    .configure(
      _.enableIntegrationTests,
      _.settings(DockerComposePlugin.settings(IT, false))
    )
    .aggregate(
      compat,
      shared,
      `shared-circe`,
      `shared-ciris`,
      `localstack-test-kit-common`,
      `localstack-aws-v1-test-kit`,
      `localstack-aws-v2-test-kit`,
      kcl,
      `kcl-logging-circe`,
      kpl,
      `kpl-logging-circe`
    )
