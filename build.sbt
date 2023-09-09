Global / onChangedBuildSource := ReloadOnSourceChanges

scalaVersion := "2.13.11"

lazy val V = new {
  val betterMonadicFor = "0.3.1"
  val kindProjector = "0.13.2"
  val circe = "0.14.6"
  val handlebars = "4.3.1"
  val http4s = "0.23.23"
  val log4cats = "2.6.0"
  val logback = "1.4.11"
}

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % V.betterMonadicFor)
addCompilerPlugin(
  "org.typelevel" % "kind-projector" % V.kindProjector cross CrossVersion.full
)

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-circe" % V.http4s,
  "org.http4s" %% "http4s-ember-client" % V.http4s,
  "org.http4s" %% "http4s-dsl" % V.http4s,
  "io.circe" %% "circe-generic" % V.circe,
  "io.circe" %% "circe-parser" % V.circe,
  "ch.qos.logback" % "logback-classic" % V.logback,
  "org.typelevel" %% "log4cats-slf4j" % V.log4cats,
  "com.github.jknack" % "handlebars-jackson2" % V.handlebars,
)

addCommandAlias("fmt", "scalafmtAll; scalafmtSbt")
