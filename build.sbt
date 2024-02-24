ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "status-checker"
  )

val mongo4CatsVersion = "0.7.2"
val zioMetricsVersion = "2.2.0"
val zioHttpVersion = "3.0.0-RC3"
val sttpZioClientVersion = "3.9.3"
val pureConfigVersion = "0.17.6"

libraryDependencies ++= Seq(
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,

  "io.github.kirill5k" %% "mongo4cats-core" % mongo4CatsVersion,
  "io.github.kirill5k" %% "mongo4cats-zio" % mongo4CatsVersion,
  "io.github.kirill5k" %% "mongo4cats-zio-embedded" % mongo4CatsVersion,

  "dev.zio" %% "zio-http" % zioHttpVersion,

  "com.softwaremill.sttp.client3" %% "zio" % sttpZioClientVersion,

  "dev.zio" %% "zio-metrics-connectors" % zioMetricsVersion,
  "dev.zio" %% "zio-metrics-connectors-prometheus" % zioMetricsVersion
)