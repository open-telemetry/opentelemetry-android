/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeText

class TelemetryDocsPluginTest {
    @TempDir
    lateinit var projectDirectory: Path

    @Test
    fun `toy project produces telemetry yaml from captured observations`() {
        projectDirectory.resolve("settings.gradle").writeText(
            """
            rootProject.name = "telemetry-docs-test"
            include(":semconv", ":toyTesting", ":toyLibrary")
            project(":toyTesting").projectDir = file("instrumentation/toy/testing")
            project(":toyLibrary").projectDir = file("instrumentation/toy/library")
            """.trimIndent(),
        )
        projectDirectory.resolve("build.gradle").writeText(
            """
            plugins {
                id "otel.telemetry-docs-root"
            }
            tasks.named("resolveTelemetryRegistry") {
                enabled = false
            }
            """.trimIndent(),
        )
        projectDirectory
            .resolve("instrumentation/toy")
            .createDirectories()
            .resolve("README.md")
            .createFile()
            .writeText("# Toy instrumentation\n")
        projectDirectory
            .resolve("semconv/model/android")
            .createDirectories()
            .resolve("events.yaml")
            .createFile()
            .writeText("groups: []\n")
        projectDirectory.resolve("semconv/build.gradle").writeText(
            """
            configurations.create("weaver") {
                canBeConsumed = true
                canBeResolved = false
            }
            def weaverExecutable = layout.buildDirectory.file("fake-weaver")
            def provideWeaverExecutable = tasks.register("provideWeaverExecutable") {
                outputs.file(weaverExecutable)
                doLast {
                    def output = weaverExecutable.get().asFile
                    output.parentFile.mkdirs()
                    output.text = "unused"
                }
            }
            artifacts.add("weaver", weaverExecutable) {
                builtBy(provideWeaverExecutable)
            }
            """.trimIndent(),
        )
        projectDirectory
            .resolve("build/telemetry-docs")
            .createDirectories()
            .resolve("resolved-registry.json")
            .createFile()
            .writeText("""{"groups":[]}""")
        projectDirectory
            .resolve("instrumentation/toy/testing")
            .createDirectories()
            .resolve("build.gradle")
            .createFile()
            .writeText(
                """
                plugins {
                    id "java"
                    id "otel.android.telemetry-docs"
                }
                repositories {
                    mavenCentral()
                }
                dependencies {
                    testImplementation "org.junit.jupiter:junit-jupiter-api:6.1.3"
                    testRuntimeOnly "org.junit.jupiter:junit-jupiter-engine:6.1.3"
                    testRuntimeOnly "org.junit.platform:junit-platform-launcher:6.1.3"
                }
                tasks.test {
                    useJUnitPlatform()
                }
                telemetryDocs {
                    scopeNames("toy.scope")
                }
                """.trimIndent(),
            )
        projectDirectory
            .resolve("instrumentation/toy/library")
            .createDirectories()
            .resolve("build.gradle")
            .writeText(
                """
                plugins {
                    id "otel.android.telemetry-docs"
                }
                telemetryDocs {
                    scopeNames("toy.scope")
                }
                """.trimIndent(),
            )
        projectDirectory
            .resolve("instrumentation/toy/library/build/telemetry-docs/observations/test")
            .createDirectories()
            .resolve("toy-testing.telemetry.json")
            .createFile()
            .writeText(
                """
                {"signals":[{
                  "type":"span",
                  "name":"toy.span",
                  "scope":"toy.scope",
                  "span_kind":"internal",
                  "attributes":[{"name":"toy.second.attribute","type":"boolean","value":true}]
                }]}
                """.trimIndent(),
            )
        projectDirectory
            .resolve("instrumentation/toy/testing/src/test/java")
            .createDirectories()
            .resolve("TelemetryCaptureTest.java")
            .createFile()
            .writeText(
                """
                import static org.junit.jupiter.api.Assertions.assertTrue;

                import java.nio.file.Files;
                import java.nio.file.Path;
                import org.junit.jupiter.api.Test;

                class TelemetryCaptureTest {
                    @Test
                    void capturesWhenEnabled() throws Exception {
                        assertTrue(Boolean.getBoolean("collectTelemetryDocs"));
                        Path outputDirectory =
                            Path.of(System.getProperty("otel.telemetryDocs.outputDirectory"));
                        Files.createDirectories(outputDirectory);
                        Files.writeString(
                            outputDirectory.resolve("toy.telemetry.json"),
                            "{\"signals\":[{" +
                                "\"type\":\"span\"," +
                                "\"name\":\"toy.span\"," +
                                "\"scope\":\"toy.scope\"," +
                                "\"span_kind\":\"internal\"," +
                                "\"attributes\":[{" +
                                    "\"name\":\"toy.attribute\"," +
                                    "\"type\":\"string\"," +
                                    "\"value\":\"secret\"" +
                                "}]}]}"
                        );
                    }
                }
                """.trimIndent(),
            )

        val testResult =
            GradleRunner
                .create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .withArguments(":toyTesting:test", "-PcollectTelemetryDocs=true", "--stacktrace")
                .build()
        assertThat(testResult.task(":toyTesting:test")?.outcome).isEqualTo(TaskOutcome.SUCCESS)

        val mergeResult =
            GradleRunner
                .create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .withArguments(":mergeToyTelemetryDocs", "--configuration-cache", "--stacktrace")
                .build()

        assertThat(mergeResult.task(":mergeToyTelemetryDocs")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(mergeResult.task(":semconv:provideWeaverExecutable")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDirectory.resolve("instrumentation/toy/telemetry.yaml").toFile()).hasContent(
            """
            # GENERATED FILE - do not edit. Regenerated by :mergeAllTelemetryDocs.
            schema_version: 1
            module: "toy"
            scopes:
              - "toy.scope"
            signals:
              - type: span
                scope: "toy.scope"
                registry_id: "unidentified"
                attributes:
                  - name: "toy.attribute"
                    type: string
                    registry: none
                  - name: "toy.second.attribute"
                    type: boolean
                    registry: none

            """.trimIndent(),
        )
        assertThat(projectDirectory.resolve("instrumentation/toy/telemetry.yaml").toFile())
            .content()
            .doesNotContain("secret")

        val cachedMergeResult =
            GradleRunner
                .create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .withArguments(":mergeToyTelemetryDocs", "--configuration-cache", "--stacktrace")
                .build()
        assertThat(cachedMergeResult.output).contains("Reusing configuration cache.")

        projectDirectory
            .resolve("instrumentation/toy/testing/build/telemetry-docs/observations")
            .toFile()
            .deleteRecursively()
        projectDirectory
            .resolve("instrumentation/toy/library/build/telemetry-docs/observations")
            .toFile()
            .deleteRecursively()
        projectDirectory.resolve("instrumentation/toy/telemetry.yaml").toFile().delete()

        val noObservationsResult =
            GradleRunner
                .create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .withArguments(":mergeToyTelemetryDocs", "--stacktrace")
                .build()
        assertThat(noObservationsResult.task(":mergeToyTelemetryDocs")?.outcome)
            .isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `capture flag is passed to Android application tests`() {
        projectDirectory.resolve("settings.gradle").writeText(
            """
            rootProject.name = "telemetry-docs-android-test"
            include(":semconv", ":testing")
            project(":testing").projectDir = file("instrumentation/android-test/testing")
            """.trimIndent(),
        )
        projectDirectory.resolve("build.gradle").writeText(
            """
            plugins {
                id "otel.telemetry-docs-root"
            }
            """.trimIndent(),
        )
        projectDirectory
            .resolve("instrumentation/android-test")
            .createDirectories()
            .resolve("README.md")
            .createFile()
            .writeText("# Android test instrumentation\n")
        projectDirectory.resolve("semconv").createDirectories().resolve("build.gradle").writeText(
            """
            configurations.create("weaver") {
                canBeConsumed = true
                canBeResolved = false
            }
            """.trimIndent(),
        )
        projectDirectory
            .resolve("instrumentation/android-test/testing")
            .createDirectories()
            .resolve("build.gradle")
            .writeText(
                """
                plugins {
                    id "com.android.application"
                    id "otel.android.telemetry-docs"
                }
                android {
                    namespace = "telemetry.docs.testing"
                    compileSdk = 37
                }
                telemetryDocs {
                    scopeNames("android.test.scope")
                }
                tasks.register("verifyCaptureArgument") {
                    doLast {
                        assert android.defaultConfig.testInstrumentationRunnerArguments["collectTelemetryDocs"] == "true"
                    }
                }
                """.trimIndent(),
            )

        val result =
            GradleRunner
                .create()
                .withProjectDir(projectDirectory.toFile())
                .withPluginClasspath()
                .withArguments(":testing:verifyCaptureArgument", "-PcollectTelemetryDocs=true", "--stacktrace")
                .build()

        assertThat(result.task(":testing:verifyCaptureArgument")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
