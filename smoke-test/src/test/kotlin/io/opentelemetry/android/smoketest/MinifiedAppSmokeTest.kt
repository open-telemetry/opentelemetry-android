/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.smoketest

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.trace.data.SpanData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.Properties
import java.util.concurrent.TimeUnit

class MinifiedAppSmokeTest {
    @Test
    fun appLaunchesAndExportsTrace() {
        val adb = findAdb()
        val apk = Paths.get(System.getProperty("smoke-test.apk"))

        assertThat(adb).isExecutable()
        assertThat(apk).isRegularFile()

        val backend = createBackend()
        try {
            backend.start()

            val endpoint =
                "http://$EMULATOR_HOST:${backend.getMappedPort(BACKEND_PORT)}"
            installApp(adb, apk)
            try {
                launchApp(adb, endpoint)

                val telemetry =
                    TelemetryRetriever(backend.getMappedPort(BACKEND_PORT), EXPORT_TIMEOUT)
                assertExpectedTrace(telemetry.waitForTraces())
            } finally {
                runAdbIgnoringFailure(adb, "uninstall", TARGET_PACKAGE)
            }
        } finally {
            backend.close()
        }
    }

    private fun createBackend(): SmokeContainer =
        SmokeContainer(FAKE_BACKEND_IMAGE)
            .withExposedPorts(BACKEND_PORT)
            .waitingFor(Wait.forHttp("/health").forPort(BACKEND_PORT))

    private fun installApp(
        adb: Path,
        apk: Path,
    ) {
        runAdb(adb, "wait-for-device")
        runAdb(adb, "install", "-r", apk.toString())
    }

    private fun launchApp(
        adb: Path,
        endpoint: String,
    ) {
        runAdb(adb, "shell", "am", "force-stop", TARGET_PACKAGE)
        runAdb(
            adb,
            "shell",
            "am",
            "start",
            "-W",
            "-n",
            "$TARGET_PACKAGE/$TARGET_ACTIVITY",
            "--es",
            OTLP_ENDPOINT_EXTRA,
            endpoint,
        )
    }

    private fun assertExpectedTrace(traces: List<SpanData>) {
        val span =
            traces.firstOrNull { it.name == SMOKE_TEST_SPAN_NAME }
                ?: throw AssertionError(
                    "Did not receive span '$SMOKE_TEST_SPAN_NAME'; " +
                        "received ${traces.map { it.name }}",
                )

        assertThat(span.instrumentationScopeInfo.name).isEqualTo(SMOKE_TEST_SCOPE_NAME)

        val resourceAttributes = span.resource.attributes
        EXPECTED_RESOURCE_ATTRIBUTES.forEach { (key, value) ->
            assertThat(resourceAttributes.get(stringKey(key))).isEqualTo(value)
        }
        listOf("telemetry.sdk.version", "device.model.name", "device.model.identifier").forEach { key ->
            assertThat(resourceAttributes.get(stringKey(key))).`as`(key).isNotBlank()
        }

        assertThat(span.attributes.get(stringKey("session.id"))).isNotBlank()
        assertThat(span.spanContext.isValid).isTrue()
    }

    private fun findAdb(): Path {
        var sdk = System.getenv("ANDROID_SDK_ROOT")
        if (sdk.isNullOrBlank()) {
            sdk = System.getenv("ANDROID_HOME")
        }
        if (sdk.isNullOrBlank()) {
            val properties = Properties()
            val localProperties =
                Paths.get(System.getProperty("smoke-test.root-dir"), "local.properties")
            Files.newInputStream(localProperties).use(properties::load)
            sdk = properties.getProperty("sdk.dir")
        }
        check(!sdk.isNullOrBlank()) { "Android SDK path is not configured" }

        val executable = if (System.getProperty("os.name").startsWith("Windows")) "adb.exe" else "adb"
        return Paths.get(sdk, "platform-tools", executable)
    }

    private fun runAdb(
        adb: Path,
        vararg arguments: String,
    ): String {
        val command = listOf(adb.toString()) + arguments
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("Timed out running: ${command.joinToString(" ")}")
        }

        val output = String(process.inputStream.readBytes(), StandardCharsets.UTF_8)
        check(process.exitValue() == 0) {
            "Command failed (${process.exitValue()}): ${command.joinToString(" ")}\n$output"
        }
        return output
    }

    private fun runAdbIgnoringFailure(
        adb: Path,
        vararg arguments: String,
    ) {
        try {
            runAdb(adb, *arguments)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (_: IOException) {
            // Best-effort cleanup.
        } catch (_: RuntimeException) {
            // Best-effort cleanup.
        }
    }

    private class SmokeContainer(
        image: String,
    ) : GenericContainer<SmokeContainer>(DockerImageName.parse(image))

    private companion object {
        const val FAKE_BACKEND_IMAGE =
            "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/" +
                "smoke-test-fake-backend:20260825.32803070924"
        const val BACKEND_PORT = 8080
        const val EMULATOR_HOST = "10.0.2.2"
        const val TARGET_PACKAGE = "io.opentelemetry.android.smoketestapp"
        const val TARGET_ACTIVITY = "$TARGET_PACKAGE.SmokeTestActivity"
        const val OTLP_ENDPOINT_EXTRA = "io.opentelemetry.android.smoketest.OTLP_ENDPOINT"
        const val SMOKE_TEST_SCOPE_NAME = "smoke-test"
        const val SMOKE_TEST_SPAN_NAME = "minified-app-smoke-test"
        const val SERVICE_NAME = "minified-smoke-test"
        val EXPORT_TIMEOUT: Duration = Duration.ofSeconds(30)
        val EXPECTED_RESOURCE_ATTRIBUTES =
            mapOf(
                "service.name" to SERVICE_NAME,
                "telemetry.sdk.name" to "opentelemetry",
                "telemetry.sdk.language" to "java",
                "os.type" to "linux",
            )
    }
}
