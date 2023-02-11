import LibraryDependencies.{Smithy4s => S4S, _}
import laika.rewrite.link._
import org.eclipse.jgit.api.MergeCommand.FastForwardMode.Merge

lazy val shared = project
  .settings(
    description := "Common shared utilities"
  )
  .enableIntegrationTests

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

lazy val `shared-localstack` = project
  .settings(
    description := "Common utilities for the localstack test-kits",
    libraryDependencies ++= Seq(Scalacheck)
  )
  .enableIntegrationTests
  .dependsOn(shared, `shared-ciris`, `shared-circe`)

lazy val `aws-v2-localstack` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the V2 AWS SDK",
    libraryDependencies ++= Seq(
      Aws.V2.kinesis,
      Aws.V2.dynamo,
      Aws.V2.cloudwatch
    )
  )
  .enableIntegrationTests
  .dependsOn(`shared-localstack`)

lazy val `aws-v1-localstack` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the V1 AWS SDK",
    libraryDependencies ++= Seq(
      Aws.V1.kinesis,
      Aws.V1.dynamo,
      Aws.V1.cloudwatch
    )
  )
  .enableIntegrationTests
  .dependsOn(`shared-localstack`)

lazy val kcl = project
  .settings(
    description := "Cats tooling for the Kinesis Client Library (KCL)",
    libraryDependencies ++= Seq(
      Aws.kcl,
      Log4Cats.slf4j
    )
  )
  .enableIntegrationTests
  .dependsOn(shared, `kinesis-client`)

lazy val `kcl-fs2` = project
  .settings(
    description := "FS2 interfaces for the KCL",
    libraryDependencies ++= Seq(FS2.core)
  )
  .enableIntegrationTests
  .dependsOn(kcl)

lazy val `kcl-http4s` = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    description := "Http4s interfaces for the KCL",
    libraryDependencies ++= Seq(
      S4S.core(smithy4sVersion.value),
      S4S.http4s(smithy4sVersion.value),
      S4S.http4sSwagger(smithy4sVersion.value),
      Http4s.emberServer
    )
  )
  .dependsOn(kcl)

lazy val `kcl-ciris` = project
  .settings(BuildInfoPlugin.buildInfoDefaultSettings)
  .settings(BuildInfoPlugin.buildInfoScopedSettings(Test))
  .settings(
    description := "Ciris tooling for the Kinesis Client Library (KCL)",
    Test / envVars ++= KCLCirisSpecVars.env,
    Test / javaOptions ++= KCLCirisSpecVars.prop,
    Test / buildInfoKeys := KCLCirisSpecVars.buildInfoKeys,
    Test / buildInfoPackage := "kinesis4cats.kcl.ciris",
    Test / buildInfoOptions += BuildInfoOption.ConstantValue
  )
  .enableIntegrationTests
  .dependsOn(kcl, `shared-ciris`, `kcl-localstack` % Test)

lazy val `kcl-fs2-ciris` = project
  .settings(BuildInfoPlugin.buildInfoDefaultSettings)
  .settings(BuildInfoPlugin.buildInfoScopedSettings(Test))
  .settings(
    description := "Ciris tooling for the Kinesis Client Library (KCL) via FS2",
    Test / envVars ++= KCLFS2CirisSpecVars.env,
    Test / javaOptions ++= KCLFS2CirisSpecVars.prop,
    Test / buildInfoKeys := KCLFS2CirisSpecVars.buildInfoKeys,
    Test / buildInfoPackage := "kinesis4cats.kcl.fs2.ciris",
    Test / buildInfoOptions += BuildInfoOption.ConstantValue
  )
  .enableIntegrationTests
  .dependsOn(`kcl-fs2`, `kcl-ciris`, `shared-ciris`)

lazy val `kcl-logging-circe` = project
  .settings(
    description := "JSON structured logging instances for the KCL, via Circe"
  )
  .enableIntegrationTests
  .dependsOn(
    kcl,
    `shared-circe`,
    `kinesis-client-localstack` % IT,
    `kcl-localstack` % IT
  )

lazy val `kcl-localstack` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KCL"
  )
  .enableIntegrationTests
  .dependsOn(`aws-v2-localstack`, kcl, `kcl-fs2`)

lazy val `kcl-tests` = project
  .enablePlugins(NoPublishPlugin, DockerImagePlugin)
  .settings(DockerImagePlugin.settings)
  .enableIntegrationTests
  .enableFunctionalTests
  .settings(
    description := "Integration Tests for the KCL",
    libraryDependencies ++= Seq(
      Logback,
      Http4s.emberClient % FunctionalTest
    ),
    assembly / assemblyMergeStrategy := {
      case "module-info.class"                        => MergeStrategy.discard
      case "AUTHORS"                                  => MergeStrategy.discard
      case "META-INF/smithy/smithy4s.tracking.smithy" => MergeStrategy.discard
      case "META-INF/smithy/manifest"                 => MergeStrategy.first
      case "scala/jdk/CollectionConverters$.class"    => MergeStrategy.first
      case PathList("google", "protobuf", _ @_*)      => MergeStrategy.first
      case PathList("codegen-resources", _ @_*)       => MergeStrategy.first
      case PathList("META-INF", xs @ _*) =>
        (xs map { _.toLowerCase }) match {
          case "services" :: xs => MergeStrategy.filterDistinctLines
          case "resources" :: "webjars" :: xs => MergeStrategy.first
          case _                              => MergeStrategy.discard
        }
      case x => MergeStrategy.defaultMergeStrategy(x)
    },
    assembly / mainClass := Some("kinesis4cats.kcl.http4s.TestKCLService")
  )
  .dependsOn(
    `kcl-http4s`,
    `kcl-localstack`,
    `kcl-logging-circe` % IT,
    `kinesis-client-localstack` % IT,
    `kinesis-client-logging-circe` % IT
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

lazy val `kpl-ciris` = project
  .settings(BuildInfoPlugin.buildInfoDefaultSettings)
  .settings(BuildInfoPlugin.buildInfoScopedSettings(Test))
  .settings(
    description := "Circe tooling for the Kinesis Producer Library (KPL)",
    Test / envVars ++= KPLCirisSpecVars.env,
    Test / javaOptions ++= KPLCirisSpecVars.prop,
    Test / buildInfoKeys := KPLCirisSpecVars.buildInfoKeys,
    Test / buildInfoPackage := "kinesis4cats.kpl.ciris",
    Test / buildInfoOptions += BuildInfoOption.ConstantValue
  )
  .enableIntegrationTests
  .dependsOn(kpl, `shared-ciris`)

lazy val `kpl-logging-circe` = project
  .settings(
    description := "JSON structured logging instances for the KPL, via Circe"
  )
  .enableIntegrationTests
  .dependsOn(kpl, `shared-circe`)

lazy val `kpl-localstack` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KPL"
  )
  .enableIntegrationTests
  .dependsOn(`aws-v1-localstack`, kpl)

lazy val `kpl-tests` = project
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the KPL"
  )
  .enableIntegrationTests
  .dependsOn(
    `kpl-localstack` % IT,
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
    `aws-v2-localstack` % IT
  )

lazy val `kinesis-client-localstack` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the Kinesis Client project"
  )
  .enableIntegrationTests
  .dependsOn(`aws-v2-localstack`, `kinesis-client`)

lazy val `kinesis-client-tests` = project
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the Kinesis Client"
  )
  .enableIntegrationTests
  .dependsOn(
    `kinesis-client-localstack` % IT,
    `kinesis-client-logging-circe` % IT
  )

lazy val `smithy4s-client-transformers` = project
  .settings(
    description := "Smithy transformers for the smithy4s-client project",
    libraryDependencies ++= Seq(
      Smithy.build(smithy4s.codegen.BuildInfo.smithyVersion)
    )
  )
  .settings(crossScalaVersions := List(Scala212))
  .enableIntegrationTests
  .enableFunctionalTests

lazy val `smithy4s-client` = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    description := "Cats tooling for the Smithy4s Kinesis Client",
    libraryDependencies ++= Seq(
      S4S.http4sAws(smithy4sVersion.value),
      Log4Cats.noop,
      Smithy.rulesEngine(smithy4s.codegen.BuildInfo.smithyVersion) % Smithy4s,
      S4S.kinesis % Smithy4s
    ),
    Compile / smithy4sAllowedNamespaces := List(
      "smithy.rules",
      "com.amazonaws.kinesis"
    ),
    Compile / smithy4sModelTransformers += "KinesisSpecTransformer",
    Compile / smithy4sAllDependenciesAsJars +=
      (`smithy4s-client-transformers` / Compile / packageBin).value
  )
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `smithy4s-client-logging-circe` = project
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    description := "JSON structured logging instances for the Smithy4s Kinesis Client, via Circe",
    libraryDependencies ++= Seq(Http4s.circe)
  )
  .enableIntegrationTests
  .dependsOn(`shared-circe`, `smithy4s-client`)

lazy val `smithy4s-client-localstack` = project
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the Smithy4s Client project"
  )
  .dependsOn(`shared-localstack`, `smithy4s-client`)

lazy val `smithy4s-client-tests` = project
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the Smithy4s Kinesis Client",
    libraryDependencies ++= Seq(Http4s.emberClient % IT, Log4Cats.slf4j % IT)
  )
  .enableIntegrationTests
  .dependsOn(
    `smithy4s-client-localstack` % IT,
    `smithy4s-client-logging-circe` % IT
  )

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    libraryDependencies ++= Seq(Log4Cats.slf4j, Http4s.emberClient),
    tlFatalWarningsInCi := false,
    tlSiteApiPackage := Some("kinesis4cats"),
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
    ),
    laikaConfig := LaikaConfig.defaults.withConfigValue(
      LinkConfig(sourceLinks =
        Seq(
          SourceLinks(
            baseUri = "https://github.com/etspaceman/kinesis4cats/blob/main/",
            suffix = "scala"
          )
        )
      )
    )
  )
  .settings(crossScalaVersions := List(Scala213))
  .dependsOn(
    shared,
    `shared-circe`,
    `shared-ciris`,
    `shared-localstack`,
    `aws-v1-localstack`,
    `aws-v2-localstack`,
    kcl,
    `kcl-fs2`,
    `kcl-http4s`,
    `kcl-ciris`,
    `kcl-fs2-ciris`,
    `kcl-logging-circe`,
    `kcl-localstack`,
    kpl,
    `kpl-ciris`,
    `kpl-logging-circe`,
    `kpl-localstack`,
    `kinesis-client`,
    `kinesis-client-logging-circe`,
    `kinesis-client-localstack`,
    `smithy4s-client`,
    `smithy4s-client-logging-circe`,
    `smithy4s-client-localstack`
  )

lazy val unidocs = project
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(
    name := "kinesis4cats-docs",
    moduleName := name.value,
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      shared,
      `shared-circe`,
      `shared-ciris`,
      `shared-localstack`,
      `aws-v1-localstack`,
      `aws-v2-localstack`,
      kcl,
      `kcl-fs2`,
      `kcl-http4s`,
      `kcl-ciris`,
      `kcl-fs2-ciris`,
      `kcl-logging-circe`,
      `kcl-localstack`,
      kpl,
      `kpl-ciris`,
      `kpl-logging-circe`,
      `kpl-localstack`,
      `kinesis-client`,
      `kinesis-client-logging-circe`,
      `kinesis-client-localstack`,
      `smithy4s-client`,
      `smithy4s-client-logging-circe`,
      `smithy4s-client-localstack`
    )
  )

lazy val allProjects = Seq(
  shared,
  `shared-circe`,
  `shared-ciris`,
  `shared-localstack`,
  `aws-v1-localstack`,
  `aws-v2-localstack`,
  kcl,
  `kcl-fs2`,
  `kcl-http4s`,
  `kcl-ciris`,
  `kcl-fs2-ciris`,
  `kcl-logging-circe`,
  `kcl-localstack`,
  `kcl-tests`,
  kpl,
  `kpl-ciris`,
  `kpl-logging-circe`,
  `kpl-localstack`,
  `kpl-tests`,
  `kinesis-client`,
  `kinesis-client-logging-circe`,
  `kinesis-client-localstack`,
  `kinesis-client-tests`,
  `smithy4s-client-transformers`,
  `smithy4s-client`,
  `smithy4s-client-logging-circe`,
  `smithy4s-client-localstack`
)

lazy val functionalTestProjects = List(`kcl-tests`)

lazy val root =
  tlCrossRootProject
    .configure(
      _.enableIntegrationTests,
      _.enableFunctionalTests
    )
    .settings(
      DockerComposePlugin.settings(IT, false, Nil),
      DockerComposePlugin
        .settings(FunctionalTest, true, functionalTestProjects),
      name := "kinesis4cats"
    )
    .aggregate(allProjects: _*)
