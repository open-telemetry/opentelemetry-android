/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import android.util.Log
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.time.Instant

class NativeCrashRecoveryReadTest {
    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
    }

    @AfterEach
    fun cleanup() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `directories at recovery paths are malformed rather than missing`() {
        val store = FileNativeCrashStore(tempDir)
        val readers =
            mapOf(
                store.crashRecordPath to { store.readCrashRecordForRecovery() },
                store.crashSnapshotPath to { store.readCrashSnapshotForRecovery(record) },
            )
        for ((path, read) in readers) {
            assertThat(read()).isEqualTo(NativeCrashRead.Missing)
            assertThat(path.mkdir()).isTrue()
            assertThat(read()).describedAs(path.name).isEqualTo(NativeCrashRead.Malformed)
            assertThat(path).isDirectory()
            val child = File(path, "keep").apply { writeText("keep") }
            assertThat(read()).isEqualTo(NativeCrashRead.Malformed)
            assertThat(child).hasContent("keep")
        }
    }

    @Test
    fun `keeps recovery reads non-destructive`() {
        val store = FileNativeCrashStore(tempDir)
        val marker = File(tempDir, "native-crash-record.properties")
        val snapshot = store.crashSnapshotPath

        assertThat(store.readCrashRecordForRecovery()).isEqualTo(NativeCrashRead.Missing)
        marker.writeText("signal.number=invalid\n")
        snapshot.writeText("invalid")

        assertThat(store.readCrashRecordForRecovery()).isEqualTo(NativeCrashRead.Malformed)
        assertThat(store.readCrashSnapshotForRecovery(record)).isEqualTo(NativeCrashRead.Malformed)
        assertThat(marker).exists()
        assertThat(snapshot).exists()
    }

    @Test
    fun `wrong snapshot lengths are malformed without consuming recovery files`() {
        val store = FileNativeCrashStore(tempDir)
        val bytes = ByteArray(NativeCrashSnapshotLayout.RECORD_SIZE)
        for (size in listOf(0, bytes.size - 1, bytes.size + 1)) {
            store.crashSnapshotPath.writeBytes(bytes.copyOf(size))

            assertThat(store.readCrashSnapshotForRecovery(record)).isEqualTo(NativeCrashRead.Malformed)
            assertThat(store.crashSnapshotPath).hasSize(size.toLong())
        }

        RandomAccessFile(store.crashSnapshotPath, "rw").use {
            it.setLength(Int.MAX_VALUE.toLong() + 1)
        }
        assertThat(store.readCrashSnapshotForRecovery(record)).isEqualTo(NativeCrashRead.Malformed)
        assertThat(store.crashSnapshotPath).hasSize(Int.MAX_VALUE.toLong() + 1)
    }

    @Test
    fun `unreadable snapshot is retryable and remains on disk`() {
        val store = FileNativeCrashStore(tempDir)
        store.crashSnapshotPath.writeBytes(ByteArray(NativeCrashSnapshotLayout.RECORD_SIZE))
        val path = store.crashSnapshotPath.toPath()
        assumeTrue(path.fileSystem.supportedFileAttributeViews().contains("posix"))
        val permissions = Files.getPosixFilePermissions(path)
        try {
            Files.setPosixFilePermissions(path, emptySet())
            assumeFalse(Files.isReadable(path))

            assertThat(store.readCrashSnapshotForRecovery(record)).isEqualTo(NativeCrashRead.Failed)
            assertThat(store.crashSnapshotPath).exists()
        } finally {
            Files.setPosixFilePermissions(path, permissions)
        }
    }

    @Test
    fun `contains snapshot parser failures without consuming crash files`() {
        val store = FileNativeCrashStore(tempDir)
        store.crashRecordPath.writeText("signal.number=11\ntimestamp.epoch_nanos=1783598400000000000\n")
        store.crashSnapshotPath.writeBytes(ByteArray(NativeCrashSnapshotLayout.RECORD_SIZE))
        mockkObject(NativeCrashSnapshotParser)
        try {
            listOf(IllegalStateException("parser failed"), UnsatisfiedLinkError("parser unavailable")).forEach { error ->
                every { NativeCrashSnapshotParser.parse(any(), any()) } throws error

                assertThat(store.readCrashSnapshotForRecovery(record)).isEqualTo(NativeCrashRead.Malformed)
                assertThat(store.readCrashRecordForRecovery()).isEqualTo(NativeCrashRead.Success(record))
                assertThat(store.crashSnapshotPath).exists()
            }
        } finally {
            unmockkObject(NativeCrashSnapshotParser)
        }
    }

    @Test
    fun `legacy reads leave non-file paths and sibling crash data alone`() {
        val store = FileNativeCrashStore(tempDir)
        assertThat(store.crashRecordPath.mkdir()).isTrue()
        store.crashSnapshotPath.writeText("keep")

        assertThat(store.readCrashRecord()).isNull()
        assertThat(store.crashRecordPath).isDirectory()
        assertThat(store.crashSnapshotPath).hasContent("keep")

        assertThat(store.crashRecordPath.delete()).isTrue()
        assertThat(store.crashSnapshotPath.delete()).isTrue()
        assertThat(store.crashSnapshotPath.mkdir()).isTrue()
        assertThat(store.readCrashSnapshot(record)).isNull()
        assertThat(store.crashSnapshotPath).isDirectory()
    }

    private companion object {
        val record = NativeCrashRecord(11, Instant.ofEpochSecond(1_783_598_400))
    }
}
