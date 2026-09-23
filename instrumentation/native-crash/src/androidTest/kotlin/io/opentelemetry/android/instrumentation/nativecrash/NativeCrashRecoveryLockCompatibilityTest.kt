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

@RunWith(AndroidJUnit4::class)
class NativeCrashRecoveryLockCompatibilityTest {
    @Test
    fun rejectsOverlapAndAllowsReacquisitionAfterClose() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = File(context.cacheDir, "native-crash-lock-compatibility")
        try {
            checkNotNull(FileNativeCrashStore(directory).acquireRecoveryLock()).use {
                assertThat(FileNativeCrashStore(directory).acquireRecoveryLock()).isNull()
            }
            checkNotNull(FileNativeCrashStore(directory).acquireRecoveryLock()).use {}
        } finally {
            directory.deleteRecursively()
        }
    }
}
