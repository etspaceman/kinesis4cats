addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.4.18")
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.4.18")
addSbtPlugin("org.typelevel" % "sbt-typelevel-mergify" % "0.4.18")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.1")
addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "1.2.0")
addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.17.4"
)
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.eed3si9n" % "sbt-projectmatrix" % "0.9.0")
addSbtPlugin("com.github.sbt" % "sbt-protobuf" % "0.7.2")
addSbtPlugin("org.scala-native" % "sbt-scala-native" % "0.4.10")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "2.0.6"
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
