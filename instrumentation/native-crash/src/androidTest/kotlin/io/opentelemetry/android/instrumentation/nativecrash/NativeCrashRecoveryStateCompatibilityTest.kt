/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class NativeCrashRecoveryStateCompatibilityTest {
    @Test
    fun replacesRecoveryStateAndSyncsItsDirectory() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, "native-crash-recovery-compatibility")
        val store = FileNativeCrashStore(directory)
        try {
            for (phase in NativeCrashRecoveryPhase.entries) {
                val state =
                    NativeCrashRecoveryState.create(
                        phase,
                        nowMillis = 2_000,
                        record =
                            if (phase == NativeCrashRecoveryPhase.MARKER_READ) {
                                null
                            } else {
                                NativeCrashRecord(11, Instant.ofEpochSecond(1))
                            },
                    )
                assertThat(store.writeRecoveryState(state)).isTrue()
                assertThat(FileNativeCrashStore(directory).readRecoveryState()).isEqualTo(NativeCrashRead.Success(state))
                assertThat(store.writeRecoveryState(state.copy(attempts = -1))).isFalse()
                assertThat(FileNativeCrashStore(directory).readRecoveryState()).isEqualTo(NativeCrashRead.Success(state))
                assertThat(File(directory, "native-crash-recovery.properties.tmp")).doesNotExist()
            }
            assertThat(store.deleteRecoveryState()).isTrue()
        } finally {
            directory.deleteRecursively()
        }
    }
}
