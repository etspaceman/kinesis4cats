addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.8.6")
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.8.6")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.8.6")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.6")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.1")
addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "1.3.2")
addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.19.7"
)
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.11.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.5.12")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.22.0")
addSbtPlugin("com.armanbilge" % "sbt-scala-native-config-brew" % "0.4.0")
addSbtPlugin(
  "com.armanbilge" % "sbt-scala-native-config-brew-github-actions" % "0.4.0"
)
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.8")

// Explicitly bumping until sbt-typelevel upgrades.
// Older versions exit sbt on compilation failures.
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.9.1")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.20",
  "org.slf4j" % "slf4j-nop" % "2.0.18"
)
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
