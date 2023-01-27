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
    description := "Common utilities for the localstack test-kits",
    libraryDependencies ++= Seq(Scalacheck)
  )
  .enableIntegrationTests
  .dependsOn(shared, `shared-ciris`, `shared-circe`)

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

lazy val `localstack-kinesis-client-test-kit` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the Kinesis Client project"
  )
  .enableIntegrationTests
  .dependsOn(`localstack-aws-v2-test-kit`, `kinesis-client`)

lazy val `localstack-kpl-test-kit` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KPL"
  )
  .enableIntegrationTests
  .dependsOn(`localstack-aws-v1-test-kit`, kpl)

lazy val kcl = project
  .settings(
    description := "Cats tooling for the Kinesis Client Library (KCL)",
    libraryDependencies ++= Seq(
      Aws.kcl,
      Log4Cats.slf4j
    )
  )
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `kcl-logging-circe` = project
  .settings(
    description := "JSON structured logging instances for the KCL, via Circe"
  )
  .enableIntegrationTests
  .dependsOn(
    kcl,
    `shared-circe`,
    `localstack-kinesis-client-test-kit` % IT,
    `localstack-kcl-test-kit` % IT,
    kcl % "it->it"
  )

lazy val `localstack-kcl-test-kit` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KCL"
  )
  .enableIntegrationTests
  .dependsOn(`localstack-aws-v2-test-kit`, kcl)

lazy val `kcl-tests` = project
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the KCL"
  )
  .enableIntegrationTests
  .dependsOn(
    `localstack-kinesis-client-test-kit` % IT,
    `kcl-logging-circe` % IT,
    `kinesis-client-logging-circe` % IT,
    `localstack-kcl-test-kit` % IT
  )

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
  .dependsOn(shared)

lazy val `kpl-logging-circe` = project
  .settings(
    description := "JSON structured logging instances for the KPL, via Circe"
  )
  .enableIntegrationTests
  .dependsOn(kpl, `shared-circe`)

lazy val `kpl-tests` = project
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the KPL"
  )
  .enableIntegrationTests
  .dependsOn(
    `localstack-kpl-test-kit` % IT,
    `kpl-logging-circe` % IT
  )

lazy val `kinesis-client` = project
  .settings(
    description := "Cats tooling for the Java Kinesis Client",
    libraryDependencies ++= Seq(
      Aws.V2.kinesis,
      Log4Cats.slf4j
    )
  )
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `kinesis-client-logging-circe` = project
  .settings(
    description := "JSON structured logging instances for the Java Kinesis Client, via Circe"
  )
  .enableIntegrationTests
  .dependsOn(
    `kinesis-client`,
    `shared-circe`,
    `localstack-aws-v2-test-kit` % IT,
    `kinesis-client` % "it->it"
  )

lazy val `kinesis-client-tests` = project
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the Kinesis Client"
  )
  .enableIntegrationTests
  .dependsOn(
    `localstack-kinesis-client-test-kit` % IT,
    `kinesis-client-logging-circe` % IT
  )

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    tlFatalWarningsInCi := false,
    tlSiteApiPackage := Some("kinesis4cats"),
    tlSitePublishBranch := Some("main"),
    tlSiteRelatedProjects ++= Seq(
      TypelevelProject.CatsEffect,
      TypelevelProject.Fs2,
      TypelevelProject.Http4s,
      "kcl" -> url(
        "https://docs.aws.amazon.com/streams/latest/dev/shared-throughput-kcl-consumers.html"
      ),
      "kpl" -> url(
        "https://docs.aws.amazon.com/streams/latest/dev/developing-producers-with-kpl.html"
      ),
      "aws-java-sdk-v1" -> url(
        "https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html"
      ),
      "aws-java-sdk-v2" -> url(
        "https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html"
      ),
      "circe" -> url("https://circe.github.io/circe/"),
      "ciris" -> url("https://cir.is/"),
      "localstack" -> url("https://localstack.cloud/")
    )
  )
  .dependsOn(
    compat,
    shared,
    `shared-circe`,
    `shared-ciris`,
    `localstack-test-kit-common`,
    `localstack-aws-v1-test-kit`,
    `localstack-aws-v2-test-kit`,
    `localstack-kinesis-client-test-kit`,
    `localstack-kpl-test-kit`,
    `localstack-kcl-test-kit`,
    kcl,
    `kcl-logging-circe`,
    `kcl-tests`,
    kpl,
    `kpl-logging-circe`,
    `kpl-tests`,
    `kinesis-client`,
    `kinesis-client-logging-circe`,
    `kinesis-client-tests`
  )

lazy val unidocs = project
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(
    name := "kinesis4cats-docs",
    moduleName := name.value,
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      compat,
      shared,
      `shared-circe`,
      `shared-ciris`,
      `localstack-test-kit-common`,
      `localstack-aws-v1-test-kit`,
      `localstack-aws-v2-test-kit`,
      `localstack-kinesis-client-test-kit`,
      `localstack-kpl-test-kit`,
      `localstack-kcl-test-kit`,
      kcl,
      `kcl-logging-circe`,
      `kcl-tests`,
      kpl,
      `kpl-logging-circe`,
      `kpl-tests`,
      `kinesis-client`,
      `kinesis-client-logging-circe`,
      `kinesis-client-tests`
    )
  )

lazy val root =
  tlCrossRootProject
    .configure(
      _.enableIntegrationTests,
      _.settings(
        DockerComposePlugin.settings(IT, false),
        name := "kinesis4cats"
      )
    )
    .aggregate(
      compat,
      shared,
      `shared-circe`,
      `shared-ciris`,
      `localstack-test-kit-common`,
      `localstack-aws-v1-test-kit`,
      `localstack-aws-v2-test-kit`,
      `localstack-kinesis-client-test-kit`,
      `localstack-kpl-test-kit`,
      `localstack-kcl-test-kit`,
      kcl,
      `kcl-logging-circe`,
      `kcl-tests`,
      kpl,
      `kpl-logging-circe`,
      `kpl-tests`,
      `kinesis-client`,
      `kinesis-client-logging-circe`,
      `kinesis-client-tests`
    )
