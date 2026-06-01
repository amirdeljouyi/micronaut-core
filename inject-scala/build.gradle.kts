import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Jar

plugins {
    id("io.micronaut.build.internal.convention-library")
    id("scala")
}

micronautBuild {
    core {
        usesMicronautTest()
    }
    binaryCompatibility.enabledAfter("5.0.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api(projects.micronautCoreProcessor)
    api(libs.managed.scala3.library)

    implementation(mnSourcegen.asm)

    compileOnly(libs.managed.scala3.compiler)

    testImplementation(projects.micronautContext)
}

tasks.withType<ScalaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_25.toString()
    targetCompatibility = JavaVersion.VERSION_25.toString()
    scalaCompileOptions.additionalParameters.add("-release:25")
    scalaCompileOptions.additionalParameters.add("-J--add-modules=java.compiler")
    scalaCompileOptions.forkOptions.jvmArgs = (scalaCompileOptions.forkOptions.jvmArgs ?: emptyList()) + "--add-modules=java.compiler"
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.DSA", "META-INF/*.RSA", "META-INF/*.SF")
    from({
        configurations.runtimeClasspath.get()
            .filter { it.isFile && it.extension == "jar" }
            .filterNot {
                it.name.startsWith("scala3-library_3-") ||
                    it.name.startsWith("scala-library-")
            }
            .map { zipTree(it) }
    })
}
