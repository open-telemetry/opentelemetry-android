/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
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
    fun `serializes recovery across store instances`() {
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
