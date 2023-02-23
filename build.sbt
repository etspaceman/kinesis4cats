import LibraryDependencies.{Smithy4s => S4S, _}
import laika.rewrite.link._
import org.eclipse.jgit.api.MergeCommand.FastForwardMode.Merge

lazy val compat = projectMatrix
  .settings(
    description := "Code to maintain compatability across major scala versions"
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .enableIntegrationTests

lazy val `kernel-tests` = projectMatrix
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Common test utilities",
    libraryDependencies ++= testDependencies
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(`shared-localstack`)

lazy val shared = projectMatrix
  .enablePlugins(ProtobufPlugin)
  .settings(
    description := "Common shared utilities",
    libraryDependencies ++= Seq(
      Aws.Aggregation.aggregator % Test,
      Aws.Aggregation.deaggregator % Test,
      Aws.kcl % Test,
      Log4Cats.slf4j % Test
    )
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(compat)

lazy val `shared-fs2` = projectMatrix
  .settings(
    description := "Common code for FS2",
    libraryDependencies ++= Seq(FS2.core)
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `shared-circe` = projectMatrix
  .settings(
    description := "Common shared utilities for Circe",
    libraryDependencies ++= Seq(
      Circe.core,
      Circe.parser
    )
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `shared-ciris` = projectMatrix
  .settings(
    description := "Common shared utilities for Ciris",
    libraryDependencies ++= Seq(Ciris.core)
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `shared-localstack` = projectMatrix
  .settings(
    description := "Common utilities for the localstack test-kits"
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(shared, `shared-ciris`, `shared-circe`)

lazy val `shared-tests` = projectMatrix
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Common test interfaces",
    libraryDependencies ++= testDependencies
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(`shared-localstack`, `shared-fs2`, `kernel-tests`)

lazy val `aws-v2-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the V2 AWS SDK",
    libraryDependencies ++= Seq(
      Aws.V2.kinesis,
      Aws.V2.dynamo,
      Aws.V2.cloudwatch
    )
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(`shared-localstack`)

lazy val `aws-v1-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the V1 AWS SDK",
    libraryDependencies ++= Seq(
      Aws.V1.kinesis,
      Aws.V1.dynamo,
      Aws.V1.cloudwatch
    )
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(`shared-localstack`)

lazy val kcl = projectMatrix
  .settings(
    description := "Cats tooling for the Kinesis Client Library (KCL)",
    libraryDependencies ++= Seq(
      Aws.kcl,
      Log4Cats.slf4j
    )
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(shared, `kinesis-client`)

lazy val `kcl-fs2` = projectMatrix
  .settings(
    description := "FS2 interfaces for the KCL",
    libraryDependencies ++= Seq(FS2.core)
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(kcl)

lazy val `kcl-http4s` = projectMatrix
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
  .jvmPlatform(allScalaVersions)
  .dependsOn(kcl)

lazy val `kcl-ciris` = projectMatrix
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
  .forkTests
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(kcl, `shared-ciris`, `kcl-localstack` % Test)

lazy val `kcl-fs2-ciris` = projectMatrix
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
  .forkTests
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(`kcl-fs2`, `kcl-ciris`, `shared-ciris`)

lazy val `kcl-logging-circe` = projectMatrix
  .settings(
    description := "JSON structured logging instances for the KCL, via Circe"
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(
    kcl,
    `shared-circe`,
    `kinesis-client-localstack` % IT,
    `kcl-localstack` % IT
  )

lazy val `kcl-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KCL"
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(`aws-v2-localstack`, kcl, `kcl-fs2`)

lazy val `kcl-tests` = projectMatrix
  .enablePlugins(NoPublishPlugin, DockerImagePlugin)
  .settings(DockerImagePlugin.settings)
  .enableIntegrationTests
  .enableFunctionalTests
  .settings(
    description := "Integration Tests for the KCL",
    libraryDependencies ++= Seq(
      Logback,
      Http4s.emberClient % FunctionalTest,
      Http4s.emberClient % IT
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
  .jvmPlatform(allScalaVersions)
  .dependsOn(
    `kcl-http4s`,
    `kcl-localstack`,
    `kcl-logging-circe` % IT,
    `kinesis-client-localstack` % IT,
    `kinesis-client-logging-circe` % IT,
    `kernel-tests` % "test;it;fun"
  )

lazy val kpl = projectMatrix
  .settings(
    description := "Cats tooling for the Kinesis Producer Library (KPL)",
    libraryDependencies ++= Seq(
      Aws.kpl,
      Log4Cats.slf4j,
      JavaXMLBind
    )
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `kpl-ciris` = projectMatrix
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
  .forkTests
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(kpl, `shared-ciris`)

lazy val `kpl-logging-circe` = projectMatrix
  .settings(
    description := "JSON structured logging instances for the KPL, via Circe"
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(kpl, `shared-circe`)

lazy val `kpl-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KPL"
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(`aws-v1-localstack`, kpl)

lazy val `kpl-tests` = projectMatrix
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the KPL"
  )
  .forkTests
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(
    `kpl-localstack` % IT,
    `kpl-logging-circe` % IT,
    `kernel-tests` % IT
  )

lazy val `kinesis-client` = projectMatrix
  .settings(
    description := "Cats tooling for the Java Kinesis Client",
    libraryDependencies ++= Seq(
      Aws.V2.kinesis,
      Log4Cats.slf4j
    )
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `kinesis-client-fs2` = projectMatrix
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(`kinesis-client`, `shared-fs2`)

lazy val `kinesis-client-logging-circe` = projectMatrix
  .settings(
    description := "JSON structured logging instances for the Java Kinesis Client, via Circe"
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(
    `kinesis-client`,
    `shared-circe`,
    `aws-v2-localstack` % IT
  )

lazy val `kinesis-client-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the Kinesis Client project"
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(`aws-v2-localstack`, `kinesis-client-fs2`)

lazy val `kinesis-client-tests` = projectMatrix
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the Kinesis Client"
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(
    `kinesis-client-localstack` % IT,
    `kinesis-client-logging-circe` % IT,
    `shared-tests` % IT,
    `kernel-tests` % IT
  )

lazy val `kinesis-client-producer-tests` = projectMatrix
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the Client Kinesis Producer",
    libraryDependencies ++= Seq(Log4Cats.slf4j % IT)
  )
  .jvmPlatform(allScalaVersions)
  .enableIntegrationTests
  .dependsOn(
    `kcl-localstack` % IT,
    `kcl-logging-circe` % IT,
    `kinesis-client-localstack` % IT,
    `kinesis-client-logging-circe` % IT,
    `shared-tests` % IT,
    `kernel-tests` % IT
  )

lazy val `smithy4s-client-transformers` = projectMatrix
  .settings(
    description := "Transformers for the smithy4s-client project",
    libraryDependencies ++= Seq(
      Smithy.build(smithy4s.codegen.BuildInfo.smithyVersion)
    )
  )
  .jvmPlatform(List(Scala212))
  .enableIntegrationTests

lazy val `smithy4s-client` = projectMatrix
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
      (`smithy4s-client-transformers`.jvm(
        Scala212
      ) / Compile / packageBin).value
  )
  .jvmPlatform(last2ScalaVersions)
  .nativePlatform(last2ScalaVersions)
  .enableIntegrationTests
  .dependsOn(shared)

lazy val `smithy4s-client-fs2` = projectMatrix
  .jvmPlatform(last2ScalaVersions)
  .nativePlatform(last2ScalaVersions)
  .enableIntegrationTests
  .dependsOn(`smithy4s-client`, `shared-fs2`)

lazy val `smithy4s-client-logging-circe` = projectMatrix
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    description := "JSON structured logging instances for the Smithy4s Kinesis Client, via Circe",
    libraryDependencies ++= Seq(Http4s.circe)
  )
  .jvmPlatform(last2ScalaVersions)
  .nativePlatform(last2ScalaVersions)
  .enableIntegrationTests
  .dependsOn(`shared-circe`, `smithy4s-client`)

lazy val `smithy4s-client-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the Smithy4s Client project"
  )
  .jvmPlatform(last2ScalaVersions)
  .nativePlatform(last2ScalaVersions)
  .dependsOn(`shared-localstack`, `smithy4s-client-fs2`)

lazy val `smithy4s-client-tests` = projectMatrix
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the Smithy4s Kinesis Client",
    libraryDependencies ++= Seq(Http4s.blazeClient % IT, Log4Cats.slf4j % IT)
  )
  .jvmPlatform(last2ScalaVersions)
  .enableIntegrationTests
  .dependsOn(
    `smithy4s-client-localstack` % IT,
    `smithy4s-client-logging-circe` % IT,
    `kernel-tests` % IT
  )

lazy val `smithy4s-client-producer-tests` = projectMatrix
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Integration Tests for the Smithy4s Kinesis Producer",
    libraryDependencies ++= Seq(Http4s.blazeClient % IT, Log4Cats.slf4j % IT)
  )
  .jvmPlatform(last2ScalaVersions)
  .enableIntegrationTests
  .dependsOn(
    `kcl-localstack` % IT,
    `kcl-logging-circe` % IT,
    `smithy4s-client-localstack` % IT,
    `smithy4s-client-logging-circe` % IT,
    `shared-tests` % IT,
    `kernel-tests` % IT
  )

lazy val docs = projectMatrix
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    libraryDependencies ++= Seq(
      Log4Cats.slf4j,
      Http4s.emberClient,
      Http4s.blazeClient
    ),
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
  .jvmPlatform(List(Scala213))
  .dependsOn(
    compat,
    `kernel-tests`,
    shared,
    `shared-fs2`,
    `shared-circe`,
    `shared-ciris`,
    `shared-localstack`,
    `shared-tests`,
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
    `kinesis-client-fs2`,
    `kinesis-client-logging-circe`,
    `kinesis-client-localstack`,
    `smithy4s-client`,
    `smithy4s-client-fs2`,
    `smithy4s-client-logging-circe`,
    `smithy4s-client-localstack`
  )

lazy val unidocs = projectMatrix
  .enablePlugins(TypelevelUnidocPlugin)
  .jvmPlatform(List(Scala213))
  .settings(
    name := "kinesis4cats-docs",
    moduleName := name.value,
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      List(
        compat,
        `kernel-tests`,
        shared,
        `shared-fs2`,
        `shared-circe`,
        `shared-ciris`,
        `shared-localstack`,
        `shared-tests`,
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
        `kinesis-client-fs2`,
        `kinesis-client-logging-circe`,
        `kinesis-client-localstack`,
        `smithy4s-client`,
        `smithy4s-client-fs2`,
        `smithy4s-client-logging-circe`,
        `smithy4s-client-localstack`
      ).map(_.jvm(Scala213).project): _*
    )
  )

lazy val allProjects = Seq(
  compat,
  `kernel-tests`,
  shared,
  `shared-fs2`,
  `shared-circe`,
  `shared-ciris`,
  `shared-localstack`,
  `shared-tests`,
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
  `kinesis-client-fs2`,
  `kinesis-client-logging-circe`,
  `kinesis-client-localstack`,
  `kinesis-client-tests`,
  `kinesis-client-producer-tests`,
  `smithy4s-client-transformers`,
  `smithy4s-client`,
  `smithy4s-client-fs2`,
  `smithy4s-client-logging-circe`,
  `smithy4s-client-localstack`,
  `smithy4s-client-tests`,
  `smithy4s-client-producer-tests`,
  unidocs
)

lazy val functionalTestProjects = List(`kcl-tests`).map(_.jvm(Scala213))

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
      name := "kinesis4cats",
      ThisBuild / mergifyLabelPaths ++= allProjects.map { x =>
        x.id -> x.base
      }.toMap
    )
    .aggregate(allProjects: _*)
