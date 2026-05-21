// tag::sbt[]
ThisBuild / scalaVersion := "3.8.3"

lazy val micronautVersion =
  sys.props.getOrElse("micronaut.version", "5.0.1-SNAPSHOT")

libraryDependencies ++= Seq(
  "io.micronaut" % "micronaut-runtime" % micronautVersion,
  "io.micronaut" % "micronaut-inject-scala" % micronautVersion % Runtime
)

addCompilerPlugin(
  "io.micronaut" % "micronaut-inject-scala" % micronautVersion
)

scalacOptions ++= Seq("-release:25", "-Yexplicit-nulls")
// end::sbt[]
