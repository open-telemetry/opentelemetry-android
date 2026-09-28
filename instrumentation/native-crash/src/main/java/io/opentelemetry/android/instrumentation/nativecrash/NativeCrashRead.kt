/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

internal sealed interface NativeCrashRead<out T> {
    data class Success<T>(
        val value: T,
    ) : NativeCrashRead<T>

    data object Missing : NativeCrashRead<Nothing>

    data object Malformed : NativeCrashRead<Nothing>

    data object Failed : NativeCrashRead<Nothing>
}
