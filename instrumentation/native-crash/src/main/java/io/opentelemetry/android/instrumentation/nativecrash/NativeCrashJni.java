/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash;

final class NativeCrashJni {
    static {
        System.loadLibrary("otel_android_native_crash");
    }

    private NativeCrashJni() {}

    static native boolean install(String crashRecordPath);
}
