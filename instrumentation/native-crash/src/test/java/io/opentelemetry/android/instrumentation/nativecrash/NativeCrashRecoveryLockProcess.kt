/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import java.io.File

internal object NativeCrashRecoveryLockProcess {
    @JvmStatic
    fun main(args: Array<String>) {
        val store = FileNativeCrashStore(File(args[0]))
        File(args[1]).writeText("waiting")
        checkNotNull(store.acquireRecoveryLock()).use {
            File(args[2]).writeText("acquired")
        }
    }
}
