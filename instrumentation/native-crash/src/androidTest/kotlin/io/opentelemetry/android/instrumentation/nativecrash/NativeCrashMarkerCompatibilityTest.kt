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
class NativeCrashMarkerCompatibilityTest {
    @Test
    fun replayLeavesCurrentLaunchNativeMarkerUntouched() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, "native-crash-launch-compatibility").apply { deleteRecursively() }
        val old = NativeCrashStorage(directory, "launch-0000000000000000001-00000000-0000-0000-0000-000000000001")
        val current = NativeCrashStorage(directory, "launch-0000000000000000002-00000000-0000-0000-0000-000000000002")
        System.loadLibrary("otel_android_native_crash")
        listOf(old, current).forEachIndexed { index, storage ->
            assertThat(storage.currentStore.writeContext(NativeCrashContext("session-$index", "1", "Android", "test"))).isTrue()
            assertThat(NativeCrashTestJni.writeCrashMarker(storage.currentStore.crashRecordPath.path, 11, 1_000_000_000L)).isTrue()
        }
        var replayed = 0
        current.replayPreviousCrashes { store ->
            assertThat(store.readContext()?.sessionId).isEqualTo("session-0")
            assertThat(store.readCrashRecord()?.signalNumber).isEqualTo(11)
            assertThat(store.deleteCrashFiles()).isTrue()
            replayed++
        }
        assertThat(replayed).isEqualTo(1)
        assertThat(current.currentStore.readContext()?.sessionId).isEqualTo("session-1")
        assertThat(current.currentStore.readCrashRecord()?.signalNumber).isEqualTo(11)
        current.replayPreviousCrashes { error("Report already replayed") }
    }

    @Test
    fun nativeWriterAndKotlinReaderUseCompatibleMarkerFormat() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory =
            File(context.cacheDir, "native-crash-marker-compatibility").apply {
                deleteRecursively()
                assertThat(mkdirs()).isTrue()
            }
        val store = FileNativeCrashStore(directory)
        val timestampNanos = 1_783_598_400_123_456_789L

        System.loadLibrary("otel_android_native_crash")
        assertThat(
            NativeCrashTestJni.writeCrashMarker(
                markerPath = store.crashRecordPath.absolutePath,
                signalNumber = 11,
                timestampEpochNanos = timestampNanos,
            ),
        ).isTrue()

        assertThat(store.readCrashRecord())
            .isEqualTo(
                NativeCrashRecord(
                    signalNumber = 11,
                    timestamp = Instant.ofEpochSecond(1_783_598_400, 123_456_789),
                ),
            )
    }
}

internal object NativeCrashTestJni {
    @JvmStatic
    external fun writeCrashMarker(
        markerPath: String,
        signalNumber: Int,
        timestampEpochNanos: Long,
    ): Boolean
}
