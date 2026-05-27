import LibraryDependencies.{Smithy4s => S4S, _}
import laika.config._
import laika.helium.Helium
import laika.helium.config.TextLink
import laika.helium.config.ThemeNavigationSection
import sbt.Package.FixedTimestamp
import sbt.Package.JarManifest
import sbt.Package.MainClass
import sbt.Package.ManifestAttributes
import sbtcrossproject.CrossPlugin.autoImport._

lazy val compat = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("compat"))
  .settings(
    description := "Code to maintain compatability across major scala versions",
    crossScalaVersions := allScalaVersions,
    scalacOptions --= Seq("-deprecation", "-Xlint:deprecation", "-Xsource:3"),
    Compile / doc / sources := Seq.empty
  )
  .nativeSettings(scalaVersion := Scala3, crossScalaVersions := Seq(Scala3))

lazy val shared = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    description := "Common shared utilities",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= testDependencies.value.map(_ % Test) ++ Seq(
      "com.thesamet.scalapb" %%% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion,
      ScalaJS.javaTime.value,
      Log4Cats.noop.value % Test
    ),
    Compile / PB.protoSources := Seq(
      baseDirectory.value.getParentFile / "src" / "main" / "protobuf"
    ),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    scalacOptions --= {
      if (tlIsScala3.value) Seq("-Wvalue-discard")
      else Nil
    }
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      Aws.kcl % Test,
      Log4Cats.slf4j % Test,
      Logback % Test,
      Aws.Aggregation.aggregator % Test,
      Aws.Aggregation.deaggregator % Test
    ),
    Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scalajvm"
  )
  .nativeEnablePlugins(ScalaNativeBrewedConfigPlugin)
  .nativeSettings(
    scalaVersion := Scala3,
    crossScalaVersions := Seq(Scala3),
    nativeBrewFormulas += "openssl",
    // scalapb codegen emits `_ <: T` which `-Xkind-projector:underscores`
    // (added by sbt-typelevel when onlyScala3 is true) rejects.
    scalacOptions --= Seq(
      "-Xkind-projector:underscores",
      "-Ykind-projector:underscores"
    ),
    scalacOptions ++= Seq("-Xkind-projector", "-language:implicitConversions")
  )
  .dependsOn(compat)

lazy val `shared-circe` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("shared-circe"))
  .settings(
    description := "Common shared utilities for Circe",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(
      Circe.core.value,
      Circe.parser.value,
      Circe.scodec.value
    )
  )
  .nativeSettings(scalaVersion := Scala3, crossScalaVersions := Seq(Scala3))
  .dependsOn(shared)

lazy val `shared-ciris` = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("shared-ciris"))
  .settings(
    description := "Common shared utilities for Ciris",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(Ciris.core.value)
  )
  .nativeSettings(scalaVersion := Scala3, crossScalaVersions := Seq(Scala3))
  .dependsOn(shared)

lazy val `shared-localstack` =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("shared-localstack"))
    .settings(
      description := "Common utilities for the localstack test-kits",
      crossScalaVersions := allScalaVersions
    )
    .nativeSettings(scalaVersion := Scala3, crossScalaVersions := Seq(Scala3))
    .dependsOn(shared, `shared-ciris`, `shared-circe`)

lazy val `shared-testkit` =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("shared-testkit"))
    .enablePlugins(NoPublishPlugin)
    .settings(
      description := "Shared scaffolding for integration tests (NoPublish)",
      crossScalaVersions := allScalaVersions,
      libraryDependencies ++= testDependencies.value,
      libraryDependencies ++= Seq(
        Circe.core.value,
        Circe.parser.value,
        Http4s.emberClient.value
      ),
      evictionErrorLevel := Level.Warn
    )
    .nativeSettings(scalaVersion := Scala3, crossScalaVersions := Seq(Scala3))
    .dependsOn(shared, `shared-circe`, `shared-localstack`)

lazy val `aws-v2-localstack` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("aws-v2-localstack"))
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the V2 AWS SDK",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(
      Aws.V2.kinesis,
      Aws.V2.dynamo,
      Aws.V2.cloudwatch
    )
  )
  .dependsOn(`shared-localstack`)

lazy val kcl = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kcl"))
  .settings(
    description := "Cats tooling for the Kinesis Client Library (KCL)",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(
      Aws.kcl,
      Log4Cats.slf4j,
      Logback % Test
    )
  )
  .dependsOn(shared, `kinesis-client`)

lazy val `kcl-http4s` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kcl-http4s"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    description := "Http4s interfaces for the KCL",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %%% "smithy4s-core" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %%% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      Http4s.emberServer.value
    ),
    Compile / smithy4sInputDirs := Seq(
      baseDirectory.value.getParentFile / "src" / "main" / "smithy"
    ),
    Compile / smithy4sSmithyLibrary := false,
    removeSmithy4sDependenciesFromManifest
  )
  .dependsOn(
    kcl,
    `shared-testkit` % Test,
    `kcl-localstack` % Test
  )

lazy val `kcl-ciris` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kcl-ciris"))
  .settings(BuildInfoPlugin.buildInfoDefaultSettings)
  .settings(BuildInfoPlugin.buildInfoScopedSettings(Test))
  .settings(
    description := "Ciris tooling for the Kinesis Client Library (KCL)",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(Logback % Test),
    Test / envVars ++= KCLFS2CirisSpecVars.env,
    Test / javaOptions ++= KCLFS2CirisSpecVars.prop,
    Test / buildInfoKeys := KCLFS2CirisSpecVars.buildInfoKeys,
    Test / buildInfoPackage := "kinesis4cats.kcl.ciris",
    Test / buildInfoOptions += BuildInfoOption.ConstantValue
  )
  .forkTests
  .dependsOn(kcl, `shared-ciris`, `kcl-localstack` % Test)

lazy val `kcl-logging-circe` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kcl-logging-circe"))
  .settings(
    description := "JSON structured logging instances for the KCL, via Circe",
    crossScalaVersions := allScalaVersions
  )
  .dependsOn(
    kcl,
    `shared-circe`,
    `kinesis-client-localstack` % Test,
    `kcl-localstack` % Test
  )

lazy val `kcl-localstack` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kcl-localstack"))
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KCL",
    crossScalaVersions := allScalaVersions
  )
  .dependsOn(
    `aws-v2-localstack`,
    kcl,
    `shared-testkit` % Test,
    `kinesis-client-localstack` % Test
  )

lazy val `kcl-http4s-test-server` = project
  .in(file("kcl-http4s-test-server"))
  .enablePlugins(NoPublishPlugin, DockerImagePlugin)
  .settings(DockerImagePlugin.settings)
  .settings(
    description := "Test server hosting the kcl-http4s service for integration tests",
    scalaVersion := Scala3,
    crossScalaVersions := Seq(Scala3),
    libraryDependencies ++= Seq(
      Http4s.blazeClient.value,
      FS2.reactiveStreams,
      Logback
    ),
    assembly / test := {},
    assembly / mainClass := Some("kinesis4cats.kcl.http4s.TestKCLService"),
    assembly / assemblyMergeStrategy := {
      case "module-info.class"                        => MergeStrategy.discard
      case "AUTHORS"                                  => MergeStrategy.discard
      case "META-INF/smithy/smithy4s.tracking.smithy" => MergeStrategy.discard
      case "META-INF/smithy/manifest"                 => MergeStrategy.first
      case "scala/jdk/CollectionConverters$.class"    => MergeStrategy.first
      case "commonMain/default/linkdata/module"       => MergeStrategy.first
      case "nativeMain/default/linkdata/module"       => MergeStrategy.first
      case "commonMain/default/manifest"              => MergeStrategy.first
      case "nativeMain/default/manifest"              => MergeStrategy.first
      case PathList("google", "protobuf", _ @_*)      => MergeStrategy.first
      case PathList("codegen-resources", _ @_*)       => MergeStrategy.first
      case PathList("io", "netty", "handler", "codec", _ @_*) =>
        MergeStrategy.first
      case PathList("META-INF", xs @ _*) =>
        (xs map { _.toLowerCase }) match {
          case "services" :: _ => MergeStrategy.filterDistinctLines
          case "resources" :: "webjars" :: _ => MergeStrategy.first
          case _                             => MergeStrategy.discard
        }
      case x => MergeStrategy.defaultMergeStrategy(x)
    },
    tlJdkRelease := Some(11)
  )
  .dependsOn(`kcl-http4s`.jvm, `kcl-localstack`.jvm)

lazy val kpl = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kpl"))
  .settings(
    description := "Cats tooling for the Kinesis Producer Library (KPL)",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(
      Aws.kpl,
      Log4Cats.slf4j,
      JavaXMLBind
    )
  )
  .dependsOn(shared)

lazy val `kpl-ciris` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kpl-ciris"))
  .settings(BuildInfoPlugin.buildInfoDefaultSettings)
  .settings(BuildInfoPlugin.buildInfoScopedSettings(Test))
  .settings(
    description := "Circe tooling for the Kinesis Producer Library (KPL)",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(Logback % Test),
    Test / envVars ++= KPLCirisSpecVars.env,
    Test / javaOptions ++= KPLCirisSpecVars.prop,
    Test / buildInfoKeys := KPLCirisSpecVars.buildInfoKeys,
    Test / buildInfoPackage := "kinesis4cats.kpl.ciris",
    Test / buildInfoOptions += BuildInfoOption.ConstantValue
  )
  .forkTests
  .dependsOn(kpl, `shared-ciris`)

lazy val `kpl-logging-circe` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kpl-logging-circe"))
  .settings(
    description := "JSON structured logging instances for the KPL, via Circe",
    crossScalaVersions := allScalaVersions
  )
  .dependsOn(kpl, `shared-circe`)

lazy val `kpl-localstack` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kpl-localstack"))
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the KPL",
    crossScalaVersions := allScalaVersions
  )
  .dependsOn(
    `aws-v2-localstack`,
    kpl,
    `shared-testkit` % Test,
    `kpl-logging-circe` % Test
  )

lazy val `kinesis-client` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kinesis-client"))
  .settings(
    description := "Cats tooling for the Java Kinesis Client",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(
      Aws.V2.kinesis,
      Aws.V2.dynamo,
      Aws.V2.cloudwatch,
      Log4Cats.slf4j,
      FS2.reactiveStreams
    )
  )
  .dependsOn(shared)

lazy val `kinesis-client-logging-circe` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kinesis-client-logging-circe"))
  .settings(
    description := "JSON structured logging instances for the Java Kinesis Client, via Circe",
    crossScalaVersions := allScalaVersions
  )
  .dependsOn(
    `kinesis-client`,
    `shared-circe`,
    `aws-v2-localstack` % Test
  )

lazy val `kinesis-client-localstack` = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("kinesis-client-localstack"))
  .settings(
    description := "A test-kit for working with Kinesis and Localstack, via the Kinesis Client project",
    crossScalaVersions := allScalaVersions
  )
  .dependsOn(
    `aws-v2-localstack`,
    `kinesis-client`,
    `shared-testkit` % Test
  )

lazy val `smithy4s-client-transformers` = project
  .in(file("smithy4s-client-transformers"))
  .enablePlugins(NoPublishPlugin)
  .settings(
    description := "Transformers for the smithy4s-client project (build-time only; not published)",
    scalaVersion := Scala3,
    crossScalaVersions := Seq(Scala3),
    libraryDependencies ++= Seq(
      Smithy.build(smithy4s.codegen.BuildInfo.smithyVersion)
    ),
    tlJdkRelease := Some(11)
  )

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

lazy val `smithy4s-client` =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("smithy4s-client"))
    .enablePlugins(Smithy4sCodegenPlugin)
    .settings(
      description := "Cats tooling for the Smithy4s Kinesis Client",
      crossScalaVersions := allScalaVersions,
      libraryDependencies ++= Seq(
        "com.disneystreaming.smithy4s" %%% "smithy4s-aws-http4s" % smithy4sVersion.value,
        Log4Cats.noop.value,
        Smithy.rulesEngine(smithy4s.codegen.BuildInfo.smithyVersion) % Smithy4s,
        S4S.kinesis % Smithy4s
      ),
      Compile / smithy4sInputDirs := Seq(
        baseDirectory.value.getParentFile / "src" / "main" / "smithy"
      ),
      Compile / smithy4sModelTransformers += "KinesisSpecTransformer",
      Compile / smithy4sAllDependenciesAsJars +=
        (`smithy4s-client-transformers` / Compile / packageBin).value,
      Compile / smithy4sSmithyLibrary := false,
      scalacOptions -= "-deprecation",
      tlJdkRelease := Some(11),
      removeSmithy4sDependenciesFromManifest
    )
    .nativeSettings(scalaVersion := Scala3, crossScalaVersions := Seq(Scala3))
    .dependsOn(shared)

lazy val `smithy4s-client-logging-circe` =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("smithy4s-client-logging-circe"))
    .settings(
      description := "JSON structured logging instances for the Smithy4s Kinesis Client, via Circe",
      crossScalaVersions := allScalaVersions,
      libraryDependencies ++= Seq(Http4s.circe.value),
      tlJdkRelease := Some(11)
    )
    .nativeSettings(scalaVersion := Scala3, crossScalaVersions := Seq(Scala3))
    .dependsOn(`shared-circe`, `smithy4s-client`)

lazy val `smithy4s-client-localstack` =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("smithy4s-client-localstack"))
    .settings(
      description := "A test-kit for working with Kinesis and Localstack, via the Smithy4s Client project",
      crossScalaVersions := allScalaVersions,
      tlJdkRelease := Some(11)
    )
    .jvmSettings(
      Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scalajvm",
      libraryDependencies ++= Seq(
        Http4s.blazeClient.value % Test,
        FS2.reactiveStreams % Test
      )
    )
    .jsSettings(
      Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scalajs",
      Test / scalaJSLinkerConfig ~= (_.withModuleKind(
        ModuleKind.CommonJSModule
      ))
    )
    .nativeEnablePlugins(ScalaNativeBrewedConfigPlugin)
    .nativeSettings(
      scalaVersion := Scala3,
      crossScalaVersions := Seq(Scala3),
      Test / unmanagedSourceDirectories += baseDirectory.value.getParentFile / "src" / "test" / "scalanative",
      nativeBrewFormulas ++= Set("s2n", "openssl"),
      Test / envVars ++= Map("S2N_DONT_MLOCK" -> "1")
    )
    .dependsOn(
      `shared-localstack`,
      `smithy4s-client`,
      `shared-testkit` % Test,
      `smithy4s-client-logging-circe` % Test
    )
    .jvmConfigure(_.dependsOn(`kcl-localstack`.jvm % Test))

lazy val feral = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("feral"))
  .settings(
    description := "Interfaces for constructing AWS Lambda functions via Feral",
    crossScalaVersions := allScalaVersions,
    libraryDependencies ++= Seq(
      Circe.core.value,
      Circe.scodec.value,
      Feral.lambda.value
    )
  )
  .dependsOn(`shared-circe`)

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .settings(
    scalaVersion := Scala3,
    crossScalaVersions := Seq(Scala3),
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
  .dependsOn(
    compat.jvm,
    shared.jvm,
    `shared-circe`.jvm,
    `shared-ciris`.jvm,
    `shared-localstack`.jvm,
    `aws-v2-localstack`.jvm,
    kcl.jvm,
    `kcl-http4s`.jvm,
    `kcl-ciris`.jvm,
    `kcl-logging-circe`.jvm,
    `kcl-localstack`.jvm,
    kpl.jvm,
    `kpl-ciris`.jvm,
    `kpl-logging-circe`.jvm,
    `kpl-localstack`.jvm,
    `kinesis-client`.jvm,
    `kinesis-client-logging-circe`.jvm,
    `kinesis-client-localstack`.jvm,
    `smithy4s-client`.jvm,
    `smithy4s-client-logging-circe`.jvm,
    `smithy4s-client-localstack`.jvm,
    feral.jvm
  )

lazy val unidocs = project
  .in(file("unidocs"))
  .enablePlugins(TypelevelUnidocPlugin)
  .settings(
    name := "kinesis4cats-docs",
    moduleName := name.value,
    scalaVersion := Scala3,
    crossScalaVersions := Seq(Scala3),
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      shared.jvm,
      `shared-circe`.jvm,
      `shared-ciris`.jvm,
      `shared-localstack`.jvm,
      `aws-v2-localstack`.jvm,
      kcl.jvm,
      `kcl-http4s`.jvm,
      `kcl-ciris`.jvm,
      `kcl-logging-circe`.jvm,
      `kcl-localstack`.jvm,
      kpl.jvm,
      `kpl-ciris`.jvm,
      `kpl-logging-circe`.jvm,
      `kpl-localstack`.jvm,
      `kinesis-client`.jvm,
      `kinesis-client-logging-circe`.jvm,
      `kinesis-client-localstack`.jvm,
      `smithy4s-client`.jvm,
      `smithy4s-client-logging-circe`.jvm,
      `smithy4s-client-localstack`.jvm,
      feral.jvm
    )
  )

lazy val allCrossProjects: Seq[sbtcrossproject.CrossProject] = Seq(
  compat,
  shared,
  `shared-circe`,
  `shared-ciris`,
  `shared-localstack`,
  `shared-testkit`,
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

lazy val allPlainProjects: Seq[Project] = Seq(
  `kcl-http4s-test-server`,
  `smithy4s-client-transformers`,
  docs,
  unidocs
)

lazy val functionalTestProjects =
  List(`kcl-http4s-test-server`)

def commonRootSettings: Seq[Setting[_]] =
  DockerComposePlugin.settings(true, functionalTestProjects) ++ Seq(
    name := "kinesis4cats",
    ThisBuild / mergifyLabelPaths ++= {
      val crossLabels = allCrossProjects.flatMap { cp =>
        val cpBase = cp.componentProjects.head.base.getParentFile
        cp.componentProjects.map(p => p.id -> cpBase)
      }
      val plainLabels = allPlainProjects.map(p => p.id -> p.base)
      (crossLabels ++ plainLabels).toMap
    }
  )

lazy val root = tlCrossRootProject
  .aggregate(allCrossProjects: _*)
  .settings(commonRootSettings)

// Plain JVM projects pinned to Scala 3. Kept outside the top-level root
// aggregate so cross-version tasks (`+update`, `+publishLocal`) don't walk
// into them under Scala 2.13, which would mix the plain projects' own _3
// libraryDependencies with the _2.13 variants of their cross-built
// dependsOn targets. CI reaches them directly via rootJVMPlain3/Test/compile.
lazy val rootJVMPlain3 = project
  .in(file(".jvm-plain-3"))
  .enablePlugins(NoPublishPlugin)
  .settings(commonRootSettings)
  .aggregate(
    `smithy4s-client-transformers`,
    docs,
    unidocs,
    `kcl-http4s-test-server`
  )
