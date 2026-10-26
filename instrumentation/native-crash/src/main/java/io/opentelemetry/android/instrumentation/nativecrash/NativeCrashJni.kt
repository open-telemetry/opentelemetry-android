/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

internal object NativeCrashJni {
    @JvmStatic external fun installForPaths(
        crashRecordPath: String,
        crashSnapshotPath: String,
    ): Boolean
}
