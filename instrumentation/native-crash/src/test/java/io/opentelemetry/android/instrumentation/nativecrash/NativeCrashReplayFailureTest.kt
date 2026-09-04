/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(IncubatingApi::class)

package io.opentelemetry.android.instrumentation.nativecrash

import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.kotlin.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.time.Instant

class NativeCrashReplayFailureTest {
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
    fun `replays marker without a stacktrace when no frame can be recovered`() {
        val store = mockStore(snapshot(programCounter = 0x3000UL))

        reporter(store).replayPreviousCrash()

        assertReplayedWithoutStacktrace()
        verify(exactly = 1) { store.deleteCrashFiles() }
    }

    @Test
    fun `formats a 32-bit frame without a build id`() {
        val store = mockStore(snapshot(NativeCrashArchitecture.ARM, buildId = null))

        reporter(store).replayPreviousCrash()

        assertThat(replayedStacktrace()).isEqualTo("#00 pc 00000120  libapp.so")
        verify(exactly = 1) { store.deleteCrashFiles() }
    }

    @Test
    fun `replays marker only when snapshot parsing fails`() {
        listOf(
            IllegalStateException("parser failed"),
            UnsatisfiedLinkError("parser unavailable"),
        ).forEach(::assertSnapshotParserFailureIsSafe)
    }

    @Test
    fun `replays marker only when snapshot unwinding fails`() {
        listOf(
            IllegalStateException("unwinder failed"),
            UnsatisfiedLinkError("unwinder unavailable"),
        ).forEach(::assertSnapshotUnwinderFailureIsSafe)
    }

    @Test
    fun `does not throw when an orphan snapshot cannot be removed`() {
        val store = FileNativeCrashStore(tempDir)
        store.crashSnapshotPath.mkdirs()
        File(store.crashSnapshotPath, "child").writeText("keeps the directory non-empty")

        reporter(store).replayPreviousCrash()

        assertThat(otelTesting.logRecords).isEmpty()
        assertThat(store.crashSnapshotPath).exists()
        verify {
            Log.w(
                any<String>(),
                "Failed to delete native crash snapshot",
                any<IOException>(),
            )
        }
    }

    @Test
    fun `still removes the snapshot when marker cleanup fails`() {
        val store = FileNativeCrashStore(tempDir)
        store.crashRecordPath.mkdirs()
        File(store.crashRecordPath, "child").writeText("keeps the directory non-empty")
        store.crashSnapshotPath.writeText("snapshot")

        assertThat(store.deleteCrashFiles()).isFalse()

        assertThat(store.crashRecordPath).exists()
        assertThat(store.crashSnapshotPath).doesNotExist()
    }

    @Test
    fun `replays marker only when snapshot file cannot be read`() {
        val store = fileStoreWithCrashFiles()

        withUnreadableFile(store.crashSnapshotPath) {
            reporter(store).replayPreviousCrash()
        }

        assertReplayedWithoutStacktrace()
        assertCrashFilesRemoved(store)
    }

    @Test
    fun `removes paired snapshot when marker file cannot be read`() {
        val store = fileStoreWithCrashFiles()

        withUnreadableFile(store.crashRecordPath) {
            reporter(store).replayPreviousCrash()
        }

        assertThat(otelTesting.logRecords).isEmpty()
        assertCrashFilesRemoved(store)
    }

    @Test
    fun `removes paired snapshot when marker properties are corrupt`() {
        val store = FileNativeCrashStore(tempDir)
        store.crashRecordPath.apply {
            parentFile?.mkdirs()
            writeText("signal.number=\\uZZZZ\n")
        }
        store.crashSnapshotPath.writeText("snapshot")

        reporter(store).replayPreviousCrash()

        assertThat(otelTesting.logRecords).isEmpty()
        assertCrashFilesRemoved(store)
    }

    private fun assertSnapshotParserFailureIsSafe(failure: Throwable) {
        val store = fileStoreWithCrashFiles()
        store.crashSnapshotPath.writeBytes(ByteArray(NativeCrashSnapshotLayout.RECORD_SIZE))
        mockkObject(NativeCrashSnapshotParser)
        try {
            every { NativeCrashSnapshotParser.parse(any(), any()) } throws failure
            reporter(store).replayPreviousCrash()
        } finally {
            unmockkObject(NativeCrashSnapshotParser)
        }

        assertReplayedWithoutStacktrace()
        assertCrashFilesRemoved(store)
        otelTesting.clearLogRecords()
    }

    private fun assertSnapshotUnwinderFailureIsSafe(failure: Throwable) {
        val snapshot = snapshot()
        val store = mockStore(snapshot)
        mockkObject(NativeCrashSnapshotUnwinder)
        try {
            every { NativeCrashSnapshotUnwinder.unwind(snapshot) } throws failure
            reporter(store).replayPreviousCrash()
        } finally {
            unmockkObject(NativeCrashSnapshotUnwinder)
        }

        assertReplayedWithoutStacktrace()
        verify(exactly = 1) { store.deleteCrashFiles() }
        otelTesting.clearLogRecords()
    }

    private fun fileStoreWithCrashFiles(): FileNativeCrashStore =
        FileNativeCrashStore(tempDir).also { store ->
            store.crashRecordPath.apply {
                parentFile?.mkdirs()
                writeText("signal.number=11\ntimestamp.epoch_nanos=1783598400000000000\n")
            }
            store.crashSnapshotPath.writeText("snapshot")
        }

    private fun mockStore(snapshot: NativeCrashSnapshot): NativeCrashStore =
        mockk<NativeCrashStore>(relaxed = true).also { store ->
            every { store.readContext() } returns null
            every { store.readCrashRecord() } returns crashRecord
            every { store.readCrashSnapshot(crashRecord) } returns snapshot
            every { store.deleteCrashFiles() } returns true
        }

    private fun snapshot(
        architecture: NativeCrashArchitecture = NativeCrashArchitecture.ARM64,
        programCounter: ULong = 0x1120UL,
        buildId: String? = "1234abcd",
    ): NativeCrashSnapshot =
        NativeCrashSnapshot(
            architecture = architecture,
            programCounter = programCounter,
            stackPointer = 0x8000UL,
            framePointer = 0UL,
            linkRegister = 0UL,
            modules =
                listOf(
                    NativeCrashModule(
                        loadBias = 0x1000UL,
                        executableStart = 0x1100UL,
                        executableEnd = 0x2000UL,
                        name = "libapp.so",
                        buildId = buildId,
                    ),
                ),
            stackStart = 0x8000UL,
            stack = byteArrayOf(),
        )

    private fun withUnreadableFile(
        file: File,
        block: () -> Unit,
    ) {
        val path = file.toPath()
        assumeTrue(path.fileSystem.supportedFileAttributeViews().contains("posix"))
        val permissions = Files.getPosixFilePermissions(path)
        try {
            Files.setPosixFilePermissions(path, emptySet<PosixFilePermission>())
            assumeFalse(Files.isReadable(path))
            block()
        } finally {
            if (Files.exists(path)) Files.setPosixFilePermissions(path, permissions)
        }
    }

    private fun assertReplayedWithoutStacktrace() {
        assertThat(otelTesting.logRecords).hasSize(1)
        assertThat(replayedStacktrace()).isNull()
    }

    private fun assertCrashFilesRemoved(store: FileNativeCrashStore) {
        assertThat(store.crashRecordPath).doesNotExist()
        assertThat(store.crashSnapshotPath).doesNotExist()
    }

    private fun replayedStacktrace(): String? =
        otelTesting.logRecords
            .single()
            .attributes
            .get(stringKey(EXCEPTION_STACKTRACE))

    private fun reporter(store: NativeCrashStore): NativeCrashReporter =
        NativeCrashReporter(
            store,
            mockk<OpenTelemetryRum> {
                every { openTelemetry } returns otelTesting.openTelemetry
            },
        )

    private companion object {
        val crashRecord = NativeCrashRecord(11, Instant.ofEpochSecond(1_783_598_400))

        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }
}
