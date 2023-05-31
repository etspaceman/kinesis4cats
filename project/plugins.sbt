addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.4.20")
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.4.20")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.4.20")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")
addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "1.3.1")
addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.17.7"
)
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.9.0")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.12")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "1.13.1")
addSbtPlugin("com.armanbilge" % "sbt-scala-native-config-brew" % "0.1.3")
addSbtPlugin(
  "com.armanbilge" % "sbt-scala-native-config-brew-github-actions" % "0.1.3"
)
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")

// Explicitly bumping until sbt-typelevel upgrades.
// Older versions exit sbt on compilation failures.
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "2.3.7")

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin" % "0.11.13",
  "org.slf4j" % "slf4j-nop" % "2.0.7"
)
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
