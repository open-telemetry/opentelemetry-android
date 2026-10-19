/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

internal enum class NativeCrashRecoveryResult {
    COMPLETE,
    RETRY_PENDING,
}
