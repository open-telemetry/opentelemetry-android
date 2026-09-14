/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import android.os.ParcelFileDescriptor
import android.system.Os
import android.util.Log
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.time.Instant
import java.util.Properties

class NativeCrashRecoveryStateTest {
    private val directoryDescriptor = mockk<FileDescriptor>()
    private val directoryHandle = mockk<ParcelFileDescriptor>()

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        mockkStatic(Os::class)
        mockkStatic(ParcelFileDescriptor::class)
        every { ParcelFileDescriptor.open(any(), ParcelFileDescriptor.MODE_READ_ONLY) } returns directoryHandle
        every { directoryHandle.fileDescriptor } returns directoryDescriptor
        justRun { Os.fsync(directoryDescriptor) }
        justRun { directoryHandle.close() }
    }

    @AfterEach
    fun cleanup() {
        unmockkStatic(Log::class)
        unmockkStatic(Os::class)
        unmockkStatic(ParcelFileDescriptor::class)
    }

    @Test
    fun `persists a versioned delivery claim`() {
        val store = FileNativeCrashStore(tempDir)
        val state =
            NativeCrashRecoveryState.create(
                NativeCrashRecoveryPhase.DELIVERY_CLAIMED,
                nowMillis = 2_000,
                record = record,
            )

        assertThat(store.writeRecoveryState(state)).isTrue()
        assertThat(store.readRecoveryState()).isEqualTo(NativeCrashRead.Success(state))
        assertThat(store.deleteRecoveryState()).isTrue()
        assertThat(store.readRecoveryState()).isEqualTo(NativeCrashRead.Missing)
    }

    @Test
    fun `syncs the directory after replacing recovery state`() {
        val store = FileNativeCrashStore(tempDir)
        val state = NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.DELIVERY_CLAIMED, 2_000, record)
        every { Os.fsync(directoryDescriptor) } answers {
            assertThat(FileNativeCrashStore(tempDir).readRecoveryState()).isEqualTo(NativeCrashRead.Success(state))
            assertThat(File(tempDir, "native-crash-recovery.properties.tmp")).doesNotExist()
        }

        assertThat(store.writeRecoveryState(state)).isTrue()

        verifyOrder {
            ParcelFileDescriptor.open(tempDir, ParcelFileDescriptor.MODE_READ_ONLY)
            Os.fsync(directoryDescriptor)
            directoryHandle.close()
        }
    }

    @Test
    fun `directory sync failure reports failure closes the descriptor and allows retry`() {
        val store = FileNativeCrashStore(tempDir)
        val state = NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.DELIVERY_CLAIMED, 2_000, record)
        every { Os.fsync(directoryDescriptor) } throws IOException("sync failed")

        assertThat(store.writeRecoveryState(state)).isFalse()
        verify(exactly = 1) { directoryHandle.close() }
        assertThat(File(tempDir, "native-crash-recovery.properties.tmp")).doesNotExist()

        justRun { Os.fsync(directoryDescriptor) }
        assertThat(store.writeRecoveryState(state)).isTrue()
        assertThat(FileNativeCrashStore(tempDir).readRecoveryState()).isEqualTo(NativeCrashRead.Success(state))
    }

    @Test
    fun `directory open and close errors fail the write`() {
        val store = FileNativeCrashStore(tempDir)
        val state = NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.MARKER_READ, 2_000)
        every { ParcelFileDescriptor.open(any(), any()) } throws IOException("open failed")
        assertThat(store.writeRecoveryState(state)).isFalse()
        verify(exactly = 0) { Os.fsync(any()) }
        verify(exactly = 0) { directoryHandle.close() }

        every { ParcelFileDescriptor.open(any(), any()) } returns directoryHandle
        every { directoryHandle.close() } throws IOException("close failed")
        assertThat(store.writeRecoveryState(state)).isFalse()
        verify(exactly = 1) { Os.fsync(directoryDescriptor) }
        verify(exactly = 1) { directoryHandle.close() }
    }

    @Test
    fun `identified recovery state only applies to the exact crash`() {
        val state = NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.DELIVERY_CLAIMED, 2_000, record)
        assertThat(state.hasIdentity()).isTrue()
        assertThat(state.matches(record)).isTrue()
        assertThat(state.appliesTo(record)).isTrue()

        for (other in listOf(
            record.copy(signalNumber = 6),
            record.copy(timestamp = record.timestamp.plusSeconds(1)),
            record.copy(timestamp = record.timestamp.plusNanos(1)),
        )) {
            assertThat(state.matches(other)).isFalse()
            assertThat(state.appliesTo(other)).isFalse()
        }
        for (partial in listOf(
            state.copy(signalNumber = null),
            state.copy(timestampEpochSecond = null),
            state.copy(timestampNano = null),
        )) {
            assertThat(partial.hasIdentity()).isFalse()
        }
    }

    @Test
    fun `marker read state survives restart and does not apply to newer crashes`() {
        val state = NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.MARKER_READ, record.timestamp.toEpochMilli())
        assertThat(FileNativeCrashStore(tempDir).writeRecoveryState(state)).isTrue()
        val restored = FileNativeCrashStore(tempDir).readRecoveryState()
        assertThat(restored).isEqualTo(NativeCrashRead.Success(state))
        val recovered = (restored as NativeCrashRead.Success).value

        assertThat(recovered.hasIdentity()).isFalse()
        assertThat(recovered.matches(record)).isFalse()
        assertThat(recovered.appliesTo(record.copy(timestamp = record.timestamp.minusNanos(1)))).isTrue()
        assertThat(recovered.appliesTo(record)).isTrue()
        assertThat(recovered.appliesTo(record.copy(timestamp = record.timestamp.plusNanos(1)))).isFalse()
        assertThat(recovered.appliesTo(record.copy(timestamp = record.timestamp.plusMillis(1)))).isFalse()
    }

    @Test
    fun `optional identity rejects invalid present fields`() {
        val store = FileNativeCrashStore(tempDir)
        val path = File(tempDir, "native-crash-recovery.properties")
        val invalidFields =
            mapOf(
                "signal.number" to listOf("", "0", "-1", "not-a-number", "2147483648"),
                "recovery.timestamp_epoch_second" to listOf("", "-1", "not-a-number", "9223372036854775808"),
                "recovery.timestamp_nano" to listOf("", "-1", "1000000000", "not-a-number"),
            )
        for (phase in listOf(NativeCrashRecoveryPhase.MARKER_READ, NativeCrashRecoveryPhase.CLEANUP, NativeCrashRecoveryPhase.ABANDONED)) {
            val state = NativeCrashRecoveryState.create(phase, 2_000)
            assertThat(store.writeRecoveryState(state)).isTrue()
            assertThat(store.readRecoveryState()).isEqualTo(NativeCrashRead.Success(state))
            for ((key, values) in invalidFields) {
                for (value in values) {
                    assertThat(store.writeRecoveryState(state)).isTrue()
                    val properties = Properties().apply { path.inputStream().use { load(it) } }
                    properties.setProperty(key, value)
                    path.outputStream().use { properties.store(it, null) }
                    val bytes = path.readBytes()

                    assertThat(store.readRecoveryState()).describedAs("%s: %s=%s", phase, key, value).isEqualTo(NativeCrashRead.Malformed)
                    assertThat(path.readBytes()).isEqualTo(bytes)
                }
            }
        }
    }

    @Test
    fun `directories at recovery paths are malformed rather than missing`() {
        val store = FileNativeCrashStore(tempDir)
        val readers =
            mapOf(
                store.crashRecordPath to { store.readCrashRecordForRecovery() },
                store.crashSnapshotPath to { store.readCrashSnapshotForRecovery(record) },
                File(tempDir, "native-crash-recovery.properties") to { store.readRecoveryState() },
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
    fun `rejects unknown and incomplete recovery state`() {
        val store = FileNativeCrashStore(tempDir)
        val path = File(tempDir, "native-crash-recovery.properties")

        path.writeText("recovery.version=2\n")
        assertThat(store.readRecoveryState()).isEqualTo(NativeCrashRead.Malformed)

        path.writeText("recovery.phase=\\uZZZZ\n")
        assertThat(store.readRecoveryState()).isEqualTo(NativeCrashRead.Malformed)
        assertThat(path).hasContent("recovery.phase=\\uZZZZ\n")

        path.writeText(
            """
            recovery.version=1
            recovery.phase=DELIVERY_CLAIMED
            recovery.attempts=0
            recovery.first_attempt_epoch_millis=2000
            signal.number=11
            """.trimIndent(),
        )
        assertThat(store.readRecoveryState()).isEqualTo(NativeCrashRead.Malformed)
    }

    @Test
    fun `invalid recovery fields are rejected without changing the stored record`() {
        val store = FileNativeCrashStore(tempDir)
        val path = File(tempDir, "native-crash-recovery.properties")
        val state = NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.SNAPSHOT_READ, 2_000, record)
        val invalidFields =
            listOf(
                "recovery.phase" to "unknown",
                "recovery.phase" to "MARKER_READ",
                "recovery.attempts" to "-1",
                "recovery.attempts" to "not-a-number",
                "recovery.first_attempt_epoch_millis" to "0",
                "recovery.first_attempt_epoch_millis" to "not-a-number",
                "signal.number" to "0",
                "recovery.timestamp_epoch_second" to "-1",
                "recovery.timestamp_nano" to "-1",
                "recovery.timestamp_nano" to "1000000000",
            )
        for ((key, value) in invalidFields) {
            assertThat(store.writeRecoveryState(state)).isTrue()
            val properties = Properties().apply { path.inputStream().use { load(it) } }
            properties.setProperty(key, value)
            path.outputStream().use { properties.store(it, null) }
            val bytes = path.readBytes()

            assertThat(store.readRecoveryState()).describedAs("%s=%s", key, value).isEqualTo(NativeCrashRead.Malformed)
            assertThat(path.readBytes()).isEqualTo(bytes)
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
    fun `failed replacement preserves the previous claim and can be retried`() {
        val store = FileNativeCrashStore(tempDir)
        val claim = NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.DELIVERY_CLAIMED, 2_000, record)
        assertThat(store.writeRecoveryState(claim)).isTrue()
        val temporaryPath = File(tempDir, "native-crash-recovery.properties.tmp")
        assertThat(temporaryPath.mkdir()).isTrue()
        val child = File(temporaryPath, "child").apply { writeText("prevents replacement") }
        val cleanup = claim.copy(phase = NativeCrashRecoveryPhase.CLEANUP, attempts = 1)

        assertThat(store.writeRecoveryState(cleanup)).isFalse()
        assertThat(FileNativeCrashStore(tempDir).readRecoveryState()).isEqualTo(NativeCrashRead.Success(claim))

        assertThat(child.delete()).isTrue()
        assertThat(temporaryPath.delete()).isTrue()
        assertThat(store.writeRecoveryState(cleanup)).isTrue()
        assertThat(FileNativeCrashStore(tempDir).readRecoveryState()).isEqualTo(NativeCrashRead.Success(cleanup))
        assertThat(temporaryPath).doesNotExist()
    }

    @Test
    fun `lock creation failure does not consume crash files`() {
        val store = FileNativeCrashStore(tempDir)
        store.crashRecordPath.writeText("signal.number=11\n")
        File(tempDir, "native-crash-recovery.lock").mkdir()

        assertThat(store.acquireRecoveryLock()).isNull()
        assertThat(store.crashRecordPath).exists()
    }

    @Test
    fun `uncreatable recovery directory fails without altering the blocking file`() {
        val blocker = File(tempDir, "blocked").apply { writeText("keep") }
        val store = FileNativeCrashStore(File(blocker, "native-crash"))

        assertThat(store.acquireRecoveryLock()).isNull()
        assertThat(store.writeRecoveryState(NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.MARKER_READ, 2_000))).isFalse()
        assertThat(blocker).hasContent("keep")
    }

    @Test
    fun `rejects an overlapping recovery lock in the same process`() {
        val first = FileNativeCrashStore(tempDir).acquireRecoveryLock()

        assertThat(first).isNotNull()
        assertThat(FileNativeCrashStore(tempDir).acquireRecoveryLock()).isNull()

        first!!.close()
        val next = FileNativeCrashStore(tempDir).acquireRecoveryLock()
        assertThat(next).isNotNull()
        next!!.close()
    }

    private companion object {
        val record = NativeCrashRecord(11, Instant.ofEpochSecond(1_783_598_400))
    }
}
