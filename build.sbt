import LibraryDependencies.{Smithy4s => S4S, _}
import laika.config._
import laika.helium.Helium
import laika.helium.config.TextLink
import laika.helium.config.ThemeNavigationSection
import sbt.Package.FixedTimestamp
import sbt.Package.JarManifest
import sbt.Package.MainClass
import sbt.Package.ManifestAttributes

lazy val compat = projectMatrix
  .settings(
    description := "Code to maintain compatability across major scala versions",
    scalacOptions --= Seq("-deprecation", "-Xlint:deprecation", "-Xsource:3"),
    Compile / doc / sources := Seq.empty
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .jsPlatform(allScalaVersions)

lazy val shared = projectMatrix
  .settings(
    description := "Common shared utilities",
    libraryDependencies ++= testDependencies.value.map(_ % Test) ++ Seq(
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      ScalaJS.javaTime.value,
      Log4Cats.noop.value % Test
    ),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    scalacOptions --= {
      if (tlIsScala3.value) Seq("-Wvalue-discard")
      else Nil
    }
  )
  .jvmPlatform(
    allScalaVersions,
    Seq(
      libraryDependencies ++= Seq(
        Aws.kcl % Test,
        Log4Cats.slf4j % Test,
        Logback % Test,
        Aws.Aggregation.aggregator % Test,
        Aws.Aggregation.deaggregator % Test
      )
    )
  )
  .nativePlatform(allScalaVersions)
  .jsPlatform(allScalaVersions)
  .dependsOn(compat)

// Workaround for https://github.com/sbt/sbt-projectmatrix/pull/79
lazy val `shared-native-213` = shared
  .native(Scala213)
  .enablePlugins(ScalaNativeBrewedConfigPlugin)
  .settings(nativeBrewFormulas += "openssl")

// Workaround for https://github.com/sbt/sbt-projectmatrix/pull/79
lazy val `shared-native-3` = shared
  .native(Scala3)
  .enablePlugins(ScalaNativeBrewedConfigPlugin)
  .settings(nativeBrewFormulas += "openssl")

lazy val `shared-circe` = projectMatrix
  .settings(
    description := "Common shared utilities for Circe",
    libraryDependencies ++= Seq(
      Circe.core.value,
      Circe.parser.value,
      Circe.scodec.value
    )
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .jsPlatform(allScalaVersions)
  .dependsOn(shared)

lazy val `shared-ciris` = projectMatrix
  .settings(
    description := "Common shared utilities for Ciris",
    libraryDependencies ++= Seq(Ciris.core.value)
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .jsPlatform(allScalaVersions)
  .dependsOn(shared)

lazy val `shared-localstack` = projectMatrix
  .settings(
    description := "Common utilities for the localstack test-kits"
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(allScalaVersions)
  .jsPlatform(allScalaVersions)
  .dependsOn(shared, `shared-ciris`, `shared-circe`)

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
  .dependsOn(`shared-localstack`)

lazy val kcl = projectMatrix
  .settings(
    description := "Cats tooling for the Kinesis Client Library (KCL)",
    libraryDependencies ++= Seq(
      Aws.kcl,
      Log4Cats.slf4j,
      Logback % Test
    )
  )
  .jvmPlatform(allScalaVersions)
  .dependsOn(shared, `kinesis-client`)

lazy val `kcl-http4s` = projectMatrix
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    description := "Http4s interfaces for the KCL",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      Http4s.emberServer.value
    ),
    Compile / smithy4sSmithyLibrary := false,
    removeSmithy4sDependenciesFromManifest
  )
  .jvmPlatform(allScalaVersions)
  .dependsOn(kcl)

lazy val `kcl-ciris` = projectMatrix
  .settings(BuildInfoPlugin.buildInfoDefaultSettings)
  .settings(BuildInfoPlugin.buildInfoScopedSettings(Test))
  .settings(
    description := "Ciris tooling for the Kinesis Client Library (KCL)",
    libraryDependencies ++= Seq(Logback % Test),
    Test / envVars ++= KCLFS2CirisSpecVars.env,
    Test / javaOptions ++= KCLFS2CirisSpecVars.prop,
    Test / buildInfoKeys := KCLFS2CirisSpecVars.buildInfoKeys,
    Test / buildInfoPackage := "kinesis4cats.kcl.ciris",
    Test / buildInfoOptions += BuildInfoOption.ConstantValue
  )
  .forkTests
  .jvmPlatform(allScalaVersions)
  .dependsOn(kcl, `shared-ciris`, `kcl-localstack` % Test)

lazy val `kcl-logging-circe` = projectMatrix
  .settings(
    description := "JSON structured logging instances for the KCL, via Circe"
  )
  .jvmPlatform(allScalaVersions)
  .dependsOn(
    kcl,
    `shared-circe`,
    `kinesis-client-localstack` % Test,
    `kcl-localstack` % Test
  )

lazy val `kcl-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KCL"
  )
  .jvmPlatform(allScalaVersions)
  .dependsOn(`aws-v2-localstack`, kcl)

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
  .dependsOn(shared)

lazy val `kpl-ciris` = projectMatrix
  .settings(BuildInfoPlugin.buildInfoDefaultSettings)
  .settings(BuildInfoPlugin.buildInfoScopedSettings(Test))
  .settings(
    description := "Circe tooling for the Kinesis Producer Library (KPL)",
    libraryDependencies ++= Seq(Logback % Test),
    Test / envVars ++= KPLCirisSpecVars.env,
    Test / javaOptions ++= KPLCirisSpecVars.prop,
    Test / buildInfoKeys := KPLCirisSpecVars.buildInfoKeys,
    Test / buildInfoPackage := "kinesis4cats.kpl.ciris",
    Test / buildInfoOptions += BuildInfoOption.ConstantValue
  )
  .forkTests
  .jvmPlatform(allScalaVersions)
  .dependsOn(kpl, `shared-ciris`)

lazy val `kpl-logging-circe` = projectMatrix
  .settings(
    description := "JSON structured logging instances for the KPL, via Circe"
  )
  .jvmPlatform(allScalaVersions)
  .dependsOn(kpl, `shared-circe`)

lazy val `kpl-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KPL"
  )
  .jvmPlatform(allScalaVersions)
  .dependsOn(`aws-v2-localstack`, kpl)

lazy val `kinesis-client` = projectMatrix
  .settings(
    description := "Cats tooling for the Java Kinesis Client",
    libraryDependencies ++= Seq(
      Aws.V2.kinesis,
      Aws.V2.dynamo,
      Aws.V2.cloudwatch,
      Log4Cats.slf4j,
      FS2.reactiveStreams
    )
  )
  .jvmPlatform(allScalaVersions)
  .dependsOn(shared)

lazy val `kinesis-client-logging-circe` = projectMatrix
  .settings(
    description := "JSON structured logging instances for the Java Kinesis Client, via Circe"
  )
  .jvmPlatform(allScalaVersions)
  .dependsOn(
    `kinesis-client`,
    `shared-circe`,
    `aws-v2-localstack` % Test
  )

lazy val `kinesis-client-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the Kinesis Client project"
  )
  .jvmPlatform(allScalaVersions)
  .dependsOn(`aws-v2-localstack`, `kinesis-client`)

lazy val `smithy4s-client-transformers` = projectMatrix
  .settings(
    description := "Transformers for the smithy4s-client project",
    libraryDependencies ++= Seq(
      Smithy.build(smithy4s.codegen.BuildInfo.smithyVersion)
    ),
    tlJdkRelease := Some(21)
  )
  .jvmPlatform(List(Scala213))

// Workaround for https://github.com/disneystreaming/smithy4s/issues/963
// as a solution for https://github.com/etspaceman/kinesis4cats/issues/123
val removeSmithy4sDependenciesFromManifest =
  Compile / packageBin / packageOptions ~= {
    _.map { opt =>
      opt match {
        case JarManifest(m) =>
          m.getMainAttributes()
            .remove(
              new java.util.jar.Attributes.Name(
                smithy4s.codegen.SMITHY4S_DEPENDENCIES
              )
            )
          opt
        case _ => opt
      }
    }
  }
lazy val `smithy4s-client` = projectMatrix
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    description := "Cats tooling for the Smithy4s Kinesis Client",
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-aws-http4s" % smithy4sVersion.value,
      Log4Cats.noop.value,
      Smithy.rulesEngine(smithy4s.codegen.BuildInfo.smithyVersion) % Smithy4s,
      S4S.kinesis % Smithy4s
      // TODO: Uncomment when fixed
      // S4S.cloudwatch % Smithy4s,
      // S4S.dynamo % Smithy4s
    ),
    Compile / smithy4sAllowedNamespaces := List(
      "smithy.rules",
      "com.amazonaws.kinesis"
    ),
    Compile / smithy4sModelTransformers += "KinesisSpecTransformer",
    Compile / smithy4sAllDependenciesAsJars +=
      (`smithy4s-client-transformers`.jvm(
        Scala213
      ) / Compile / packageBin).value,
    Compile / smithy4sSmithyLibrary := false,
    scalacOptions -= "-deprecation",
    tlJdkRelease := Some(21),
    removeSmithy4sDependenciesFromManifest
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(Seq(Scala3))
  .jsPlatform(allScalaVersions)
  .dependsOn(shared)

lazy val `smithy4s-client-logging-circe` = projectMatrix
  .settings(
    description := "JSON structured logging instances for the Smithy4s Kinesis Client, via Circe",
    libraryDependencies ++= Seq(Http4s.circe.value),
    tlJdkRelease := Some(21)
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(Seq(Scala3))
  .jsPlatform(allScalaVersions)
  .dependsOn(`shared-circe`, `smithy4s-client`)

lazy val `smithy4s-client-localstack` = projectMatrix
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the Smithy4s Client project",
    tlJdkRelease := Some(21)
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(Seq(Scala3))
  .jsPlatform(allScalaVersions)
  .dependsOn(`shared-localstack`, `smithy4s-client`)

lazy val integrationTestsJvmSettings: Seq[Setting[_]] = Seq(
  Test / fork := true,
  libraryDependencies ++= Seq(
    Http4s.blazeClient.value % Test,
    FS2.reactiveStreams % Test,
    Logback
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
        case "services" :: xs               => MergeStrategy.filterDistinctLines
        case "resources" :: "webjars" :: xs => MergeStrategy.first
        case _                              => MergeStrategy.discard
      }
    case x => MergeStrategy.defaultMergeStrategy(x)
  },
  assembly / mainClass := Some("kinesis4cats.kcl.http4s.TestKCLService"),
  tlJdkRelease := Some(21)
)

lazy val feral = projectMatrix
  .settings(
    description := "Interfaces for constructing AWS Lambda functions via Feral",
    libraryDependencies ++= Seq(
      Circe.core.value,
      Circe.scodec.value,
      Feral.lambda.value
    )
  )
  .jvmPlatform(allScalaVersions)
  .jsPlatform(allScalaVersions)
  .dependsOn(`shared-circe`)

lazy val integrationTestsJvmDependencies = List(
  `kcl-http4s`,
  `kcl-localstack`
)

lazy val integrationTestsJvmTestDependencies = List(
  `kcl-logging-circe`,
  `kinesis-client-localstack`,
  `kinesis-client-logging-circe`,
  `kpl-localstack`,
  `kpl-logging-circe`
)

lazy val `integration-tests` = projectMatrix
  .enablePlugins(NoPublishPlugin, DockerImagePlugin)
  .settings(DockerImagePlugin.settings)
  .settings(
    description := "Integration Tests for Kinesis4Cats",
    libraryDependencies ++= Seq(
      Http4s.emberClient.value % Test
    )
  )
  .jvmPlatform(allScalaVersions)
  .nativePlatform(Seq(Scala3))
  .jsPlatform(
    allScalaVersions,
    Nil,
    _.settings(
      scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
    )
  )
  .dependsOn(
    `smithy4s-client-localstack` % Test,
    `smithy4s-client-logging-circe` % Test
  )

// Workaround for https://github.com/sbt/sbt-projectmatrix/pull/79
lazy val `integration-tests-native` = `integration-tests`
  .native(Scala3)
  .enablePlugins(ScalaNativeBrewedConfigPlugin)
  .settings(
    nativeBrewFormulas ++= Set("s2n", "openssl"),
    Test / envVars ++= Map("S2N_DONT_MLOCK" -> "1")
  )

lazy val `integration-tests-jvm-213` = `integration-tests`
  .jvm(Scala213)
  .settings(integrationTestsJvmSettings)
  .dependsOn(
    (integrationTestsJvmDependencies.map(x =>
      ClasspathDependency(x.jvm(Scala213).project, None)
    ) ++ integrationTestsJvmTestDependencies.map(x =>
      ClasspathDependency(x.jvm(Scala213).project, Some(Test.name))
    )): _*
  )

lazy val `integration-tests-jvm-3` = `integration-tests`
  .jvm(Scala3)
  .settings(integrationTestsJvmSettings)
  .dependsOn(
    (integrationTestsJvmDependencies.map(x =>
      ClasspathDependency(x.jvm(Scala3).project, None)
    ) ++ integrationTestsJvmTestDependencies.map(x =>
      ClasspathDependency(x.jvm(Scala3).project, Some(Test.name))
    )): _*
  )

lazy val docs = projectMatrix
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    libraryDependencies ++= Seq(
      Log4Cats.slf4j,
      Http4s.emberClient.value,
      Http4s.blazeClient.value
    ),
    tlFatalWarnings := false,
    tlSiteApiPackage := Some("kinesis4cats"),
    tlSiteHelium := tlSiteHelium.value.site
      .mainNavigation(appendLinks =
        Seq(
          ThemeNavigationSection(
            "Related Projects",
            TextLink.external(
              "https://docs.aws.amazon.com/streams/latest/dev/shared-throughput-kcl-consumers.html",
              "kcl"
            ),
            TextLink.external(
              "https://docs.aws.amazon.com/streams/latest/dev/developing-producers-with-kpl.html",
              "kpl"
            ),
            TextLink.external(
              "https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html",
              "aws-java-sdk-v1"
            ),
            TextLink.external(
              "https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html",
              "aws-java-sdk-v2"
            ),
            TextLink.external(
              "https://circe.github.io/circe/",
              "circe"
            ),
            TextLink.external(
              "https://cir.is/",
              "ciris"
            ),
            TextLink.external(
              "https://localstack.cloud/",
              "localstack"
            ),
            TextLink.external(
              "https://github.com/typelevel/feral",
              "feral"
            )
          )
        )
      ),
    laikaConfig := LaikaConfig.defaults.withConfigValue(
      LinkConfig.empty.addSourceLinks(
        SourceLinks(
          baseUri = "https://github.com/etspaceman/kinesis4cats/blob/main/",
          suffix = "scala"
        )
      )
    )
  )
  .jvmPlatform(List(Scala213))
  .dependsOn(
    compat,
    shared,
    `shared-circe`,
    `shared-ciris`,
    `shared-localstack`,
    `aws-v1-localstack`,
    `aws-v2-localstack`,
    kcl,
    `kcl-http4s`,
    `kcl-ciris`,
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
    `smithy4s-client-localstack`,
    feral
  )

lazy val unidocs = projectMatrix
  .enablePlugins(TypelevelUnidocPlugin)
  .jvmPlatform(List(Scala213))
  .settings(
    name := "kinesis4cats-docs",
    moduleName := name.value,
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      List(
        shared,
        `shared-circe`,
        `shared-ciris`,
        `shared-localstack`,
        `aws-v1-localstack`,
        `aws-v2-localstack`,
        kcl,
        `kcl-http4s`,
        `kcl-ciris`,
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
        `smithy4s-client-localstack`,
        feral
      ).map(_.jvm(Scala213).project): _*
    )
  )

lazy val allProjects = Seq(
  compat,
  shared,
  `shared-circe`,
  `shared-ciris`,
  `shared-localstack`,
  `aws-v1-localstack`,
  `aws-v2-localstack`,
  kcl,
  `kcl-http4s`,
  `kcl-ciris`,
  `kcl-logging-circe`,
  `kcl-localstack`,
  kpl,
  `kpl-ciris`,
  `kpl-logging-circe`,
  `kpl-localstack`,
  `kinesis-client`,
  `kinesis-client-logging-circe`,
  `kinesis-client-localstack`,
  `smithy4s-client-transformers`,
  `smithy4s-client`,
  `smithy4s-client-logging-circe`,
  `smithy4s-client-localstack`,
  feral,
  `integration-tests`,
  unidocs
)

lazy val functionalTestProjects = List(`integration-tests`).map(_.jvm(Scala213))

def commonRootSettings: Seq[Setting[_]] =
  DockerComposePlugin.settings(true, functionalTestProjects) ++ Seq(
    name := "kinesis4cats",
    ThisBuild / mergifyLabelPaths ++= allProjects.map { x =>
      x.id -> x.base
    }.toMap
  )

lazy val root = project
  .in(file("."))
  .enablePlugins(NoPublishPlugin)
  .settings(commonRootSettings)
  .aggregate(allProjects.flatMap(_.projectRefs): _*)

lazy val `root-jvm-213` = project
  .enablePlugins(NoPublishPlugin)
  .settings(commonRootSettings)
  .aggregate(
    allProjects.flatMap(
      _.filterProjects(
        Seq(VirtualAxis.jvm, VirtualAxis.ScalaVersionAxis(Scala213, "2.13"))
      ).map(_.project)
    ): _*
  )

lazy val `root-jvm-3` = project
  .enablePlugins(NoPublishPlugin)
  .settings(commonRootSettings)
  .aggregate(
    allProjects.flatMap(
      _.filterProjects(
        Seq(VirtualAxis.jvm, VirtualAxis.ScalaVersionAxis(Scala3, "3.2"))
      ).map(_.project)
    ): _*
  )

lazy val `root-js-213` = project
  .enablePlugins(NoPublishPlugin)
  .settings(commonRootSettings)
  .aggregate(
    allProjects.flatMap(
      _.filterProjects(
        Seq(VirtualAxis.js, VirtualAxis.ScalaVersionAxis(Scala213, "2.13"))
      ).map(_.project)
    ): _*
  )

lazy val `root-js-3` = project
  .enablePlugins(NoPublishPlugin)
  .settings(commonRootSettings)
  .aggregate(
    allProjects.flatMap(
      _.filterProjects(
        Seq(VirtualAxis.js, VirtualAxis.ScalaVersionAxis(Scala3, "3.2"))
      ).map(_.project)
    ): _*
  )

lazy val `root-native-213` = project
  .enablePlugins(NoPublishPlugin)
  .settings(commonRootSettings)
  .aggregate(
    allProjects.flatMap(
      _.filterProjects(
        Seq(VirtualAxis.native, VirtualAxis.ScalaVersionAxis(Scala213, "2.13"))
      ).map(_.project)
    ): _*
  )

lazy val `root-native-3` = project
  .enablePlugins(NoPublishPlugin)
  .settings(commonRootSettings)
  .aggregate(
    allProjects.flatMap(
      _.filterProjects(
        Seq(VirtualAxis.native, VirtualAxis.ScalaVersionAxis(Scala3, "3.2"))
      ).map(_.project)
    ): _*
  )

lazy val rootProjects = List(
  `root-jvm-213`,
  `root-jvm-3`,
  `root-js-213`,
  `root-js-3`,
  `root-native-213`,
  `root-native-3`
).map(_.id)

ThisBuild / githubWorkflowBuildMatrixAdditions += "project" -> rootProjects
