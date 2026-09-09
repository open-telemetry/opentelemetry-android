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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

class NativeCrashRecoveryStateTest {
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
    fun `rejects unknown and incomplete recovery state`() {
        val store = FileNativeCrashStore(tempDir)
        val path = File(tempDir, "native-crash-recovery.properties")

        path.writeText("recovery.version=2\n")
        assertThat(store.readRecoveryState()).isEqualTo(NativeCrashRead.Malformed)

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
