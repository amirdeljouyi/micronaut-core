// tag::gradle[]
plugins {
    scala
}

dependencies {
    implementation("org.scala-lang:scala3-library_3")
    scalaCompilerPlugins("io.micronaut:micronaut-inject-scala")
    runtimeOnly("io.micronaut:micronaut-inject-scala")
}

tasks.withType<org.gradle.api.tasks.scala.ScalaCompile>().configureEach {
    scalaCompileOptions.additionalParameters.add("-release:25")
    scalaCompileOptions.additionalParameters.add("-Yexplicit-nulls")
}
// end::gradle[]
