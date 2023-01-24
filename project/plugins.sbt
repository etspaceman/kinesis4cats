addSbtPlugin("org.typelevel" % "sbt-typelevel" % "0.4.18")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.6")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.1.0")
addSbtPlugin("org.portable-scala" % "sbt-crossproject" % "1.2.0")

libraryDependencies += "org.slf4j" % "slf4j-nop" % "2.0.6"
