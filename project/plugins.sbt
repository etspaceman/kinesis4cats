addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.8.2")
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.8.2")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.8.2")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.5")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.4")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")
addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "1.3.2")
addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.18.41"
)
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.17")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.19.0")
addSbtPlugin("com.armanbilge" % "sbt-scala-native-config-brew" % "0.4.0")
addSbtPlugin(
  "com.armanbilge" % "sbt-scala-native-config-brew-github-actions" % "0.4.0"
)
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8")

// Explicitly bumping until sbt-typelevel upgrades.
// Older versions exit sbt on compilation failures.
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.7.2")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.15",
  "org.slf4j" % "slf4j-nop" % "2.0.17"
)
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
