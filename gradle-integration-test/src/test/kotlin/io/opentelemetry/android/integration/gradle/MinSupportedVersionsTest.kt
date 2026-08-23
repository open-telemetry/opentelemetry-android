/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.integration.gradle

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Builds a real consumer app against the snapshot artifacts in mavenLocal, using the minimum
 * JDK, Gradle, AGP, Kotlin and compileSdk that VERSIONING.md promises to support.
 */
class MinSupportedVersionsTest {
    @Test
    fun `consumer app builds at the minimum supported toolchain`(
        @TempDir tmp: Path,
    ) {
        assembleFixture("consumer-app", tmp)
    }

    @Test
    fun `bytecode weaving runs at the minimum supported toolchain`(
        @TempDir tmp: Path,
    ) {
        assembleFixture("weaving-app", tmp)
    }

    private fun assembleFixture(
        fixtureName: String,
        tmp: Path,
    ) {
        requireMinSupportedJdk()
        copyRecursively(File(systemProperty("fixtureSrcDir"), fixtureName).toPath(), tmp)
        writeLocalProperties(tmp)

        GradleRunner
            .create()
            .withProjectDir(tmp.toFile())
            .withGradleVersion(systemProperty("minSupportedGradle"))
            .withArguments(
                "-PcatalogPath=${systemProperty("catalogPath")}",
                "-PbomVersion=${systemProperty("bomVersion")}",
                "-PminSdk=${systemProperty("minSdk")}",
                "-PminCompileSdk=${systemProperty("minCompileSdk")}",
                "assembleRelease",
                "--stacktrace",
                "--no-configuration-cache",
            ).forwardOutput()
            .build()
    }

    // TestKit starts the fixture build on the JVM running this test, so a fixture that builds on a
    // newer JDK than the documented minimum proves nothing about that minimum.
    private fun requireMinSupportedJdk() {
        val expected = systemProperty("minSupportedJdk").toInt()
        val actual = Runtime.version().feature()
        check(actual == expected) {
            "fixtures must build on JDK $expected, the documented minimum, but this test runs on JDK $actual"
        }
    }

    private fun writeLocalProperties(projectDir: Path) {
        val sdkDir = System.getProperty("androidSdkDir").orEmpty()
        if (sdkDir.isNotBlank()) {
            Files.write(
                projectDir.resolve("local.properties"),
                "sdk.dir=$sdkDir\n".toByteArray(),
            )
        }
    }

    private fun systemProperty(name: String): String =
        requireNotNull(System.getProperty(name)) {
            "system property '$name' was not set by the Gradle test task"
        }

    private fun copyRecursively(
        source: Path,
        target: Path,
    ) {
        Files.walk(source).use { stream ->
            stream.forEach { path ->
                val destination = target.resolve(source.relativize(path).toString())
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination)
                } else {
                    Files.createDirectories(destination.parent)
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
