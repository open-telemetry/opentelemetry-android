/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(IncubatingApi::class)

package io.opentelemetry.android.instrumentation.nativecrash

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.android.session.SessionPublisher
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger

class NativeCrashReporterTest {
    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @AfterEach
    fun cleanup() {
        otelTesting.clearLogRecords()
        unmockkStatic(Log::class)
    }

    @Test
    fun `has a stable instrumentation name`() {
        assertThat(NativeCrashInstrumentation().name).isEqualTo("native-crash")
    }

    @Test
    fun `installs replay and session observer using the application context`() {
        val store = FileNativeCrashStore(tempDir)
        writeMarker(signalNumber = 11, timestampNanos = 1_783_598_400_000_000_000L)
        val packageInfo =
            PackageInfo().apply {
                versionName = "1.2.3"
            }
        val packageManager = mockk<PackageManager>()
        val applicationContext = mockk<Context>()
        val context = mockk<Context>()
        every { context.applicationContext } returns applicationContext
        every { applicationContext.packageManager } returns packageManager
        every { applicationContext.packageName } returns "test.app"
        every { packageManager.getPackageInfo("test.app", 0) } returns packageInfo
        val sessionProvider =
            RecordingSessionProvider(
                sessionId = "install-session",
                sessionStartedOnRegistration = "started-session",
            )
        val instrumentation =
            NativeCrashInstrumentation(
                storeFactory = { actualContext ->
                    assertThat(actualContext).isSameAs(applicationContext)
                    store
                },
                executor = directExecutor,
            )

        instrumentation.install(context, fakeRum(sessionProvider))

        assertThat(otelTesting.logRecords).hasSize(1)
        assertThat(store.readContext())
            .isEqualTo(
                NativeCrashContext(
                    sessionId = "started-session",
                    serviceVersion = "1.2.3",
                    osName = "Android",
                    osVersion = Build.VERSION.RELEASE,
                ),
            )
        assertThat(sessionProvider.observer).isNotNull()
    }

    @Test
    fun `replays a valid marker with crash-time context`() {
        val store = FileNativeCrashStore(tempDir)
        writeMarker(
            signalNumber = 11,
            timestampNanos = 1_783_598_400_000_000_000L,
            crashContext = crashContext("crashed"),
        )

        reporter(store).install(crashContext("current"))

        assertThat(otelTesting.logRecords).hasSize(1)
        val log = otelTesting.logRecords.single()
        assertThat(log.eventName).isEqualTo("app.crash")
        assertThat(log.timestampEpochNanos).isEqualTo(1_783_598_400_000_000_000L)
        assertThat(log.attributes.get(stringKey(EXCEPTION_TYPE))).isEqualTo("SIGSEGV")
        assertThat(log.attributes.get(stringKey(EXCEPTION_MESSAGE)))
            .isEqualTo("Native crash signal SIGSEGV (11)")
        assertThat(log.attributes.get(stringKey(SESSION_ID))).isEqualTo("crashed-session")
        assertThat(log.attributes.get(stringKey(SERVICE_VERSION))).isEqualTo("crashed-version")
        assertThat(log.attributes.get(stringKey(OS_NAME))).isEqualTo("crashed-os")
        assertThat(log.attributes.get(stringKey(OS_VERSION))).isEqualTo("crashed-os-version")
        assertThat(store.readCrashRecords()).isEmpty()
        assertThat(store.readContext()).isEqualTo(crashContext("current"))
    }

    @Test
    fun `replays a valid marker without crash-time context`() {
        val store = FileNativeCrashStore(tempDir)
        writeMarker(signalNumber = 11, timestampNanos = 1_783_598_400_000_000_000L)

        reporter(store).install(crashContext("current"))

        val attributes = otelTesting.logRecords.single().attributes
        assertThat(attributes.get(stringKey(SESSION_ID))).isNull()
        assertThat(attributes.get(stringKey(SERVICE_VERSION))).isNull()
        assertThat(attributes.get(stringKey(OS_NAME))).isNull()
        assertThat(attributes.get(stringKey(OS_VERSION))).isNull()
    }

    @Test
    fun `replays a valid marker when crash-time context is corrupt`() {
        val store = FileNativeCrashStore(tempDir)
        contextFile().apply {
            parentFile?.mkdirs()
            writeText("session.id=\\uZZZZ\n")
        }
        writeMarker(signalNumber = 11, timestampNanos = 1_783_598_400_000_000_000L)

        reporter(store).install(crashContext("current"))

        assertThat(
            otelTesting.logRecords
                .single()
                .attributes
                .get(stringKey(SESSION_ID)),
        ).isNull()
        assertThat(store.readContext()).isEqualTo(crashContext("current"))
    }

    @Test
    fun `maps native signal numbers to names`() {
        val signalNumbers = listOf(4, 5, 6, 7, 8, 11, 15)

        val signalNames = signalNumbers.map { NativeCrashRecord(it, Instant.EPOCH).signalName }

        assertThat(signalNames)
            .containsExactly("SIGILL", "SIGTRAP", "SIGABRT", "SIGBUS", "SIGFPE", "SIGSEGV", "SIG15")
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
    fun `ignores and removes markers with invalid timestamps`() {
        val store = FileNativeCrashStore(tempDir)
        val invalidTimestamps = listOf(null, "not-a-number", "0", "-1")

        invalidTimestamps.forEach { timestamp ->
            markerFile().apply {
                parentFile?.mkdirs()
                writeText(
                    buildString {
                        appendLine("signal.number=11")
                        timestamp?.let { appendLine("timestamp.epoch_nanos=$it") }
                    },
                )
            }

            assertThat(store.readCrashRecords()).isEmpty()
            assertThat(markerFile()).doesNotExist()
        }
    }

    @Test
    fun `does not replay an already consumed marker`() {
        val store = FileNativeCrashStore(tempDir)
        writeMarker(
            signalNumber = 6,
            timestampNanos = 1_783_598_400_000_000_000L,
            crashContext = crashContext("crashed"),
        )
        val reporter = reporter(store)

        reporter.install(crashContext("current"))
        reporter.install(crashContext("later"))

        assertThat(otelTesting.logRecords).hasSize(1)
        assertThat(store.readContext()).isEqualTo(crashContext("later"))
    }

    @Test
    fun `replays multiple markers without one malformed marker blocking the others`() {
        val store = FileNativeCrashStore(tempDir)
        writeMarker(
            signalNumber = 11,
            timestampNanos = 1_783_598_400_000_000_000L,
            markerId = "one",
            crashContext = crashContext("first"),
        )
        markerFile("two").writeText("signal.number=invalid\ntimestamp.epoch_nanos=123\n")
        writeMarker(
            signalNumber = 6,
            timestampNanos = 1_783_598_401_000_000_000L,
            markerId = "three",
            crashContext = crashContext("third"),
        )

        reporter(store).install(crashContext("current"))

        assertThat(otelTesting.logRecords.map { it.attributes.get(stringKey(EXCEPTION_TYPE)) })
            .containsExactly("SIGSEGV", "SIGABRT")
        assertThat(otelTesting.logRecords.map { it.attributes.get(stringKey(SESSION_ID)) })
            .containsExactly("first-session", "third-session")
        assertThat(store.readCrashRecords()).isEmpty()
        assertThat(markerFile("two")).doesNotExist()
    }

    @Test
    fun `removes a marker after repeated read failures`() {
        val readAttempts = AtomicInteger()
        val store =
            FileNativeCrashStore(
                directory = tempDir,
                crashRecordReader = {
                    readAttempts.incrementAndGet()
                    throw IOException("temporary read failure")
                },
            )
        writeMarker(signalNumber = 11, timestampNanos = 1_783_598_400_000_000_000L)

        repeat(2) {
            assertThat(store.readCrashRecords()).isEmpty()
            assertThat(markerFile()).exists()
        }
        assertThat(store.readCrashRecords()).isEmpty()

        assertThat(readAttempts).hasValue(3)
        assertThat(markerFile()).doesNotExist()
        assertThat(readFailureFile()).doesNotExist()
    }

    @Test
    fun `clears the failure count after a marker can be read`() {
        val readAttempts = AtomicInteger()
        val store =
            FileNativeCrashStore(
                directory = tempDir,
                crashRecordReader = { path ->
                    if (readAttempts.incrementAndGet() == 1) {
                        throw IOException("temporary read failure")
                    }
                    Properties().also { properties ->
                        FileInputStream(path).use { properties.load(it) }
                    }
                },
            )
        writeMarker(signalNumber = 11, timestampNanos = 1_783_598_400_000_000_000L)

        assertThat(store.readCrashRecords()).isEmpty()
        assertThat(readFailureFile()).exists()

        assertThat(store.readCrashRecords()).hasSize(1)
        assertThat(readFailureFile()).doesNotExist()
    }

    @Test
    fun `updates persisted context when the session changes`() {
        val store = FileNativeCrashStore(tempDir)
        val observer = NativeCrashSessionObserver(store, crashContext("original"), directExecutor)

        observer.onSessionStarted(session("new-session"), session("old-session"))

        assertThat(store.readContext()).isEqualTo(crashContext("original").copy(sessionId = "new-session"))
    }

    private fun reporter(store: NativeCrashStore): NativeCrashReporter = NativeCrashReporter(store, fakeRum())

    private fun writeMarker(
        signalNumber: Int,
        timestampNanos: Long,
        markerId: String = "test",
        crashContext: NativeCrashContext? = null,
    ) {
        val path =
            markerFile(markerId).apply {
                parentFile?.mkdirs()
            }
        val properties =
            Properties().apply {
                setProperty("signal.number", signalNumber.toString())
                setProperty("timestamp.epoch_nanos", timestampNanos.toString())
                crashContext?.sessionId?.let { setProperty(SESSION_ID, it) }
                crashContext?.serviceVersion?.let { setProperty(SERVICE_VERSION, it) }
                crashContext?.osName?.let { setProperty(OS_NAME, it) }
                crashContext?.osVersion?.let { setProperty(OS_VERSION, it) }
            }
        FileOutputStream(path).use { properties.store(it, null) }
    }

    private fun markerFile(markerId: String = "test"): File = File(tempDir, "native-crash-record-$markerId.properties")

    private fun readFailureFile(markerId: String = "test"): File = File(tempDir, "native-crash-record-$markerId.properties.read-failures")

    private fun contextFile(): File = File(tempDir, "native-crash-context.properties")

    private fun session(sessionId: String): Session =
        object : Session {
            override val id: String = sessionId
            override val startTimestamp: Long = 0
        }

    private fun crashContext(prefix: String): NativeCrashContext =
        NativeCrashContext(
            sessionId = "$prefix-session",
            serviceVersion = "$prefix-version",
            osName = "$prefix-os",
            osVersion = "$prefix-os-version",
        )

    private fun fakeRum(sessionProvider: SessionProvider = SessionProvider { "current-session" }): OpenTelemetryRum =
        object : OpenTelemetryRum {
            override val openTelemetry: OpenTelemetry = otelTesting.openTelemetry
            override val sessionProvider: SessionProvider = sessionProvider
            override val clock: Clock = Clock.getDefault()

            override fun emitEvent(
                eventName: String,
                body: String,
                attributes: Attributes,
            ) {}

            override fun shutdown() {}
        }

    private class RecordingSessionProvider(
        private val sessionId: String,
        private val sessionStartedOnRegistration: String? = null,
    ) : SessionProvider,
        SessionPublisher {
        var observer: SessionObserver? = null

        override fun getSessionId(): String = sessionId

        override fun addObserver(observer: SessionObserver) {
            this.observer = observer
            sessionStartedOnRegistration?.let { newSessionId ->
                observer.onSessionStarted(
                    object : Session {
                        override val id: String = newSessionId
                        override val startTimestamp: Long = 0
                    },
                    object : Session {
                        override val id: String = sessionId
                        override val startTimestamp: Long = 0
                    },
                )
            }
        }
    }

    private companion object {
        val directExecutor = java.util.concurrent.Executor { command -> command.run() }

        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }
}
