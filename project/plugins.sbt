addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.4.18")
addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.4.18")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.7")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.0")
addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "1.2.0")
addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % "0.17.3"
)
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "2.0.6"
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
