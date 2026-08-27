import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    kotlin("jvm")
    id("otel.spotless-conventions")
}

val javaVersion = rootProject.extra["java_version"] as JavaVersion
val targetJvm = rootProject.extra["jvm_target"] as JvmTarget

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin {
    compilerOptions {
        jvmTarget.set(targetJvm)
    }
}

dependencies {
    testImplementation(platform(libs.opentelemetry.platform.alpha))
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit5.jupiter.api)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.opentelemetry.testing.common)
    testImplementation(libs.testcontainers)
    testRuntimeOnly(libs.junit5.jupiter.engine)
    testRuntimeOnly(libs.junit5.platform.launcher)
}

val smokeTestAppApk =
    project(":smoke-test-app").layout.buildDirectory.file(
        "outputs/apk/release/smoke-test-app-release.apk",
    )
val rootDirectory = rootProject.layout.projectDirectory.asFile.absolutePath
val smokeTestRequested =
    gradle.startParameter.taskNames.any { taskName ->
        taskName == "smokeTest" || taskName.endsWith(":smokeTest")
    }

tasks.named<Test>("test") {
    enabled = false
}

tasks.register<Test>("smokeTest") {
    description = "Runs the minified app smoke test on a connected Android emulator."
    group = "verification"
    enabled = smokeTestRequested
    useJUnitPlatform()
    testClassesDirs =
        sourceSets.test
            .get()
            .output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    dependsOn(":smoke-test-app:assembleRelease")
    systemProperty("smoke-test.apk", smokeTestAppApk.get().asFile.absolutePath)
    systemProperty("smoke-test.root-dir", rootDirectory)
}
