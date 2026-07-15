/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash;

final class NativeCrashJni {
    private NativeCrashJni() {}

    static native boolean install(String crashRecordPath);
}
