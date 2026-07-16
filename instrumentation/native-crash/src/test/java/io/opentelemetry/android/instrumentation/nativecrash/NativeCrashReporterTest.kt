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
import io.mockk.verify
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
import java.io.FileOutputStream
import java.time.Instant
import java.util.Properties

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
    fun `installs the signal handler after replay and current context persistence`() {
        val store = FileNativeCrashStore(tempDir)
        writeMarker(signalNumber = 11, timestampNanos = 1_783_598_400_000_000_000L)
        writeContext(crashContext("crashed"))
        val packageManager = mockk<PackageManager>()
        val applicationContext = mockk<Context>()
        val context = mockk<Context>()
        every { context.applicationContext } returns applicationContext
        every { applicationContext.packageManager } returns packageManager
        every { applicationContext.packageName } returns "test.app"
        every { packageManager.getPackageInfo("test.app", 0) } throws
            PackageManager.NameNotFoundException()
        var queuedTask: Runnable? = null
        var storeCreated = false
        var signalHandlerInstalled = false
        val instrumentation =
            NativeCrashInstrumentation(
                storeFactory = {
                    storeCreated = true
                    store
                },
                executor = { task -> queuedTask = task },
                signalHandlerInstaller = {
                    assertThat(store.readCrashRecord()).isNull()
                    assertThat(store.readContext()?.sessionId).isEqualTo("current-session")
                    assertThat(
                        otelTesting.logRecords
                            .single()
                            .attributes
                            .get(stringKey(SESSION_ID)),
                    ).isEqualTo("crashed-session")
                    signalHandlerInstalled = true
                    true
                },
            )

        instrumentation.install(context, fakeRum())

        assertThat(storeCreated).isFalse()
        assertThat(signalHandlerInstalled).isFalse()
        assertThat(store.readCrashRecord()).isNotNull()
        assertThat(queuedTask).isNotNull()

        queuedTask!!.run()

        assertThat(storeCreated).isTrue()
        assertThat(signalHandlerInstalled).isTrue()
    }

    @Test
    fun `does not install the signal handler when current context cannot be persisted`() {
        val marker = File(tempDir, "native-crash-record.properties")
        val store = mockk<NativeCrashStore>(relaxed = true)
        every { store.crashRecordPath } returns marker
        every { store.readCrashRecord() } returns null
        every { store.readContext() } returns null
        every { store.writeContext(any()) } returns false
        val packageManager = mockk<PackageManager>()
        val applicationContext = mockk<Context>()
        val context = mockk<Context>()
        every { context.applicationContext } returns applicationContext
        every { applicationContext.packageManager } returns packageManager
        every { applicationContext.packageName } returns "test.app"
        every { packageManager.getPackageInfo("test.app", 0) } throws
            PackageManager.NameNotFoundException()
        var signalHandlerInstalled = false
        val instrumentation =
            NativeCrashInstrumentation(
                storeFactory = { store },
                executor = directExecutor,
                signalHandlerInstaller = {
                    signalHandlerInstalled = true
                    true
                },
            )

        instrumentation.install(context, fakeRum())

        assertThat(signalHandlerInstalled).isFalse()
        verify {
            Log.w(
                any<String>(),
                "Native crash signal handler disabled because crash context could not be persisted",
            )
        }
    }

    @Test
    fun `prepares the marker directory before native installation`() {
        val marker = File(tempDir, "missing/native-crash-record.properties")

        assertThat(prepareCrashRecordDirectory(marker)).isTrue()
        assertThat(marker.parentFile).isDirectory()
    }

    @Test
    fun `rejects an unusable marker directory`() {
        val fileInsteadOfDirectory = File(tempDir, "not-a-directory").apply { writeText("occupied") }
        val marker = File(fileInsteadOfDirectory, "native-crash-record.properties")

        assertThat(prepareCrashRecordDirectory(marker)).isFalse()
        assertThat(marker.parentFile).isFile()
    }

    @Test
    fun `loads the native library and installs the handler`() {
        val marker = File(tempDir, "missing/native-crash-record.properties")
        var loadedLibrary: String? = null
        var installedPath: String? = null
        val installer =
            JniNativeSignalHandlerInstaller(
                loadLibrary = { loadedLibrary = it },
                nativeInstall = { path ->
                    installedPath = path
                    true
                },
            )

        assertThat(installer.install(marker)).isTrue()
        assertThat(loadedLibrary).isEqualTo("otel_android_native_crash")
        assertThat(installedPath).isEqualTo(marker.absolutePath)
    }

    @Test
    fun `does not load the native library when the marker directory is unusable`() {
        val fileInsteadOfDirectory = File(tempDir, "not-a-directory").apply { writeText("occupied") }
        val marker = File(fileInsteadOfDirectory, "native-crash-record.properties")
        var libraryLoaded = false
        val installer =
            JniNativeSignalHandlerInstaller(
                loadLibrary = { libraryLoaded = true },
                nativeInstall = { true },
            )

        assertThat(installer.install(marker)).isFalse()
        assertThat(libraryLoaded).isFalse()
    }

    @Test
    fun `returns false when the native library cannot be loaded`() {
        val marker = File(tempDir, "native-crash-record.properties")
        val failure = UnsatisfiedLinkError("missing library")
        val installer =
            JniNativeSignalHandlerInstaller(
                loadLibrary = { throw failure },
                nativeInstall = { true },
            )

        assertThat(installer.install(marker)).isFalse()
        verify {
            Log.w(
                any<String>(),
                "Failed to load native crash signal handler",
                failure,
            )
        }
    }

    @Test
    fun `installs replay and session observer using the application context`() {
        val store = FileNativeCrashStore(tempDir)
        var installedMarkerPath: File? = null
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
                signalHandlerInstaller = { markerPath ->
                    installedMarkerPath = markerPath
                    true
                },
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
        assertThat(installedMarkerPath).isEqualTo(store.crashRecordPath)
    }

    @Test
    fun `continues installation when the native handler is unavailable`() {
        val store = FileNativeCrashStore(tempDir)
        val packageManager = mockk<PackageManager>()
        val applicationContext = mockk<Context>()
        val context = mockk<Context>()
        every { context.applicationContext } returns applicationContext
        every { applicationContext.packageManager } returns packageManager
        every { applicationContext.packageName } returns "test.app"
        every { packageManager.getPackageInfo("test.app", 0) } throws
            PackageManager.NameNotFoundException()
        val sessionProvider = RecordingSessionProvider(sessionId = "install-session")
        val instrumentation =
            NativeCrashInstrumentation(
                storeFactory = { store },
                executor = directExecutor,
                signalHandlerInstaller = { false },
            )

        instrumentation.install(context, fakeRum(sessionProvider))

        assertThat(store.readContext()?.sessionId).isEqualTo("install-session")
        assertThat(sessionProvider.observer).isNotNull()
        verify {
            Log.w(
                any<String>(),
                "Failed to install native crash signal handler",
            )
        }
    }

    @Test
    fun `replays a valid marker with crash-time context`() {
        val store = FileNativeCrashStore(tempDir)
        writeMarker(
            signalNumber = 11,
            timestampNanos = 1_783_598_400_000_000_000L,
        )
        writeContext(crashContext("crashed"))

        reporter(store).replayPreviousCrash()
        store.writeContext(crashContext("current"))

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
        assertThat(store.readCrashRecord()).isNull()
        assertThat(store.readContext()).isEqualTo(crashContext("current"))
    }

    @Test
    fun `reads the native marker format with nanosecond precision`() {
        val store = FileNativeCrashStore(tempDir)
        markerFile().apply {
            parentFile?.mkdirs()
            writeText(
                "signal.number=11\n" +
                    "timestamp.epoch_nanos=1783598400123456789\n",
            )
        }

        assertThat(store.readCrashRecord())
            .isEqualTo(
                NativeCrashRecord(
                    signalNumber = 11,
                    timestamp = Instant.ofEpochSecond(1_783_598_400, 123_456_789),
                ),
            )
    }

    @Test
    fun `replays a valid marker without crash-time context`() {
        val store = FileNativeCrashStore(tempDir)
        writeMarker(signalNumber = 11, timestampNanos = 1_783_598_400_000_000_000L)

        reporter(store).replayPreviousCrash()

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

        reporter(store).replayPreviousCrash()
        store.writeContext(crashContext("current"))

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
        val signalNumbers = listOf(4, 5, 6, 7, 8, 11, 31, 15)

        val signalNames = signalNumbers.map { NativeCrashRecord(it, Instant.EPOCH).signalName }

        assertThat(signalNames)
            .containsExactly(
                "SIGILL",
                "SIGTRAP",
                "SIGABRT",
                "SIGBUS",
                "SIGFPE",
                "SIGSEGV",
                "SIGSYS",
                "SIG15",
            )
    }

    @Test
    fun `does not emit when the marker is missing`() {
        val store = FileNativeCrashStore(tempDir)

        reporter(store).replayPreviousCrash()
        store.writeContext(crashContext("current"))

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

        reporter(store).replayPreviousCrash()
        store.writeContext(crashContext("current"))

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

            assertThat(store.readCrashRecord()).isNull()
            assertThat(markerFile()).doesNotExist()
        }
    }

    @Test
    fun `does not replay an already consumed marker`() {
        val store = FileNativeCrashStore(tempDir)
        writeMarker(
            signalNumber = 6,
            timestampNanos = 1_783_598_400_000_000_000L,
        )
        writeContext(crashContext("crashed"))
        val reporter = reporter(store)

        reporter.replayPreviousCrash()
        store.writeContext(crashContext("current"))
        reporter.replayPreviousCrash()
        store.writeContext(crashContext("later"))

        assertThat(otelTesting.logRecords).hasSize(1)
        assertThat(store.readContext()).isEqualTo(crashContext("later"))
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
    ) {
        val path =
            markerFile().apply {
                parentFile?.mkdirs()
            }
        val properties =
            Properties().apply {
                setProperty("signal.number", signalNumber.toString())
                setProperty("timestamp.epoch_nanos", timestampNanos.toString())
            }
        FileOutputStream(path).use { properties.store(it, null) }
    }

    private fun writeContext(context: NativeCrashContext) {
        FileNativeCrashStore(tempDir).writeContext(context)
    }

    private fun markerFile(): File = File(tempDir, "native-crash-record.properties")

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
