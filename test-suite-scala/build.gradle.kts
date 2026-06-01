import org.gradle.api.tasks.scala.ScalaCompile

plugins {
    id("io.micronaut.build.internal.convention-test-library")
    id("scala")
}

micronautBuild {
    core {
        usesMicronautTestJunit()
        usesMicronautTestSpock()
    }
}

dependencies {
    implementation(libs.managed.scala3.library)
    compileOnly(projects.micronautCoreProcessor)
    scalaCompilerPlugins(projects.micronautInjectScala)

    testImplementation(projects.micronautContext)
    testImplementation(projects.micronautContextPropagation)
    testImplementation(projects.micronautHttpServerNetty)
    testImplementation(projects.micronautHttpClient)
    testImplementation(projects.micronautRuntime)
    testImplementation(projects.micronautInject)
    testImplementation(projects.micronautManagement)
    testRuntimeOnly(projects.micronautInjectScala)
    testImplementation(projects.micronautJacksonDatabind)
    testImplementation(libs.managed.reactor)
    testImplementation(libs.managed.groovy.templates)
    testImplementation(libs.logbook.netty)
    testImplementation(platform(libs.test.boms.micronaut.validation))
    testImplementation(libs.micronaut.validation) {
        exclude(group = "io.micronaut")
    }
    testImplementation(libs.micronaut.validation.processor) {
        exclude(group = "io.micronaut")
    }
}

tasks.withType<ScalaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_25.toString()
    targetCompatibility = JavaVersion.VERSION_25.toString()
    scalaCompileOptions.additionalParameters.add("-release:25")
    scalaCompileOptions.additionalParameters.add("-J--add-modules=java.compiler")
    scalaCompileOptions.forkOptions.memoryMaximumSize = "6g"
    scalaCompileOptions.forkOptions.jvmArgs = (scalaCompileOptions.forkOptions.jvmArgs ?: emptyList()) + "--add-modules=java.compiler"
}
