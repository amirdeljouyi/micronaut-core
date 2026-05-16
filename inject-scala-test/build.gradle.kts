import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

plugins {
    id("io.micronaut.build.internal.convention-library")
}

micronautBuild {
    binaryCompatibility.enabledAfter("5.0.1")
}

dependencies {
    compileOnly(projects.micronautInjectScala)

    api(projects.micronautContext)
    api(projects.micronautInjectScala)
    api(projects.micronautRetry)
    api(libs.managed.groovy)
    api(libs.managed.scala3.compiler)
    api(libs.managed.scala3.library)
    api(libs.spock) {
        exclude(module = "groovy-all")
    }

    testImplementation(platform(libs.test.boms.micronaut.validation))
    testImplementation(libs.micronaut.validation) {
        exclude(group = "io.micronaut")
    }
    testImplementation(libs.micronaut.validation.processor) {
        exclude(group = "io.micronaut")
    }
}

tasks.withType<Test>().configureEach {
    dependsOn(project(":micronaut-inject-scala").tasks.named("jar"))
    doFirst {
        systemProperty(
            "micronaut.scala.plugin.jar",
            project(":micronaut-inject-scala").tasks.named<Jar>("jar").flatMap { it.archiveFile }.get().asFile.absolutePath
        )
        systemProperty("micronaut.scala.test.classpath", sourceSets.test.get().runtimeClasspath.asPath)
    }
}
