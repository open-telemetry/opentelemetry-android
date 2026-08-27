import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    kotlin("jvm")
    id("otel.spotless-conventions")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(platform(libs.opentelemetry.platform.alpha))
    testImplementation(libs.assertj.core)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.opentelemetry.testing.common)
    testImplementation(libs.testcontainers)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
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
    onlyIf { smokeTestRequested }
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
