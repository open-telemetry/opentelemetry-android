/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(IncubatingApi::class)

package io.opentelemetry.android.instrumentation.nativecrash

import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_BUILD_ID
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_MESSAGE
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_TYPE
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_NAME
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_VERSION
import io.opentelemetry.kotlin.semconv.ServiceAttributes.SERVICE_VERSION
import io.opentelemetry.kotlin.semconv.SessionAttributes.SESSION_ID
import io.opentelemetry.sdk.common.Clock
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import java.io.File

class NativeCrashReporterTest {
    @TempDir
    lateinit var tempDir: File

    @AfterEach
    fun cleanup() {
        otelTesting.clearLogRecords()
    }

    @Test
    fun `replays a valid marker with crash-time context`() {
        val store = FileNativeCrashStore(tempDir)
        store.writeContext(crashContext("crashed"))
        writeMarker(signalNumber = 11, timestampNanos = 1_783_598_400_000_000_000L)

        reporter(store).install(crashContext("current"))

        assertThat(otelTesting.logRecords).hasSize(1)
        val log = otelTesting.logRecords.single()
        assertThat(log.eventName).isEqualTo("app.crash")
        assertThat(log.timestampEpochNanos).isEqualTo(1_783_598_400_000_000_000L)
        assertThat(log.attributes.get(stringKey(EXCEPTION_TYPE))).isEqualTo("signal.SIGSEGV")
        assertThat(log.attributes.get(stringKey(EXCEPTION_MESSAGE)))
            .isEqualTo("Native crash signal SIGSEGV (11)")
        assertThat(log.attributes.get(stringKey(SESSION_ID))).isEqualTo("crashed-session")
        assertThat(log.attributes.get(stringKey(APP_BUILD_ID))).isEqualTo("crashed-build")
        assertThat(log.attributes.get(stringKey(SERVICE_VERSION))).isEqualTo("crashed-version")
        assertThat(log.attributes.get(stringKey(OS_NAME))).isEqualTo("crashed-os")
        assertThat(log.attributes.get(stringKey(OS_VERSION))).isEqualTo("crashed-os-version")
        assertThat(store.readCrashRecord()).isNull()
        assertThat(store.readContext()).isEqualTo(crashContext("current"))
    }

    @Test
    fun `does not emit when the marker is missing`() {
        val store = FileNativeCrashStore(tempDir)

        reporter(store).install(crashContext("current"))

        assertThat(otelTesting.logRecords).isEmpty()
        assertThat(store.readContext()).isEqualTo(crashContext("current"))
    }

    @Test
    fun `ignores and removes a malformed marker`() {
        val store = FileNativeCrashStore(tempDir)
        markerFile().apply {
            parentFile?.mkdirs()
            writeText("signal.number=not-a-number\ntimestamp.epoch_nanos=123\n")
        }

        reporter(store).install(crashContext("current"))

        assertThat(otelTesting.logRecords).isEmpty()
        assertThat(markerFile()).doesNotExist()
    }

    @Test
    fun `does not replay an already consumed marker`() {
        val store = FileNativeCrashStore(tempDir)
        store.writeContext(crashContext("crashed"))
        writeMarker(signalNumber = 6, timestampNanos = 1_783_598_400_000_000_000L)
        val reporter = reporter(store)

        reporter.install(crashContext("current"))
        reporter.install(crashContext("later"))

        assertThat(otelTesting.logRecords).hasSize(1)
        assertThat(store.readContext()).isEqualTo(crashContext("later"))
    }

    @Test
    fun `updates persisted context when the session changes`() {
        val store = FileNativeCrashStore(tempDir)
        val observer = NativeCrashSessionObserver(store, crashContext("original"))

        observer.onSessionStarted(session("new-session"), session("old-session"))

        assertThat(store.readContext()).isEqualTo(crashContext("original").copy(sessionId = "new-session"))
    }

    private fun reporter(store: NativeCrashStore): NativeCrashReporter = NativeCrashReporter(store, fakeRum())

    private fun writeMarker(
        signalNumber: Int,
        timestampNanos: Long,
    ) {
        markerFile().apply {
            parentFile?.mkdirs()
            writeText("signal.number=$signalNumber\ntimestamp.epoch_nanos=$timestampNanos\n")
        }
    }

    private fun markerFile(): File = File(tempDir, "native-crash.properties")

    private fun session(sessionId: String): Session =
        object : Session {
            override val id: String = sessionId
            override val startTimestamp: Long = 0
        }

    private fun crashContext(prefix: String): NativeCrashContext =
        NativeCrashContext(
            sessionId = "$prefix-session",
            appBuildId = "$prefix-build",
            serviceVersion = "$prefix-version",
            osName = "$prefix-os",
            osVersion = "$prefix-os-version",
        )

    private fun fakeRum(): OpenTelemetryRum =
        object : OpenTelemetryRum {
            override val openTelemetry: OpenTelemetry = otelTesting.openTelemetry
            override val sessionProvider: SessionProvider = SessionProvider { "current-session" }
            override val clock: Clock = Clock.getDefault()

            override fun emitEvent(
                eventName: String,
                body: String,
                attributes: Attributes,
            ) {}

            override fun shutdown() {}
        }

    private companion object {
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }
}
