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
