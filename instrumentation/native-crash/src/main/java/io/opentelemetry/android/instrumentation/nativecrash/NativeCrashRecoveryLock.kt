/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

internal fun interface NativeCrashRecoveryLock : AutoCloseable {
    override fun close()
}
