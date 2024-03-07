/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import androidx.annotation.NonNull;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class CrashReportingExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Consumer<CrashDetails> crashSender;
    private final SdkLoggerProvider sdkLoggerProvider;
    private final Thread.UncaughtExceptionHandler existingHandler;

    CrashReportingExceptionHandler(
            Consumer<CrashDetails> crashSender,
            SdkLoggerProvider sdkLoggerProvider,
            Thread.UncaughtExceptionHandler existingHandler) {
        this.crashSender = crashSender;
        this.sdkLoggerProvider = sdkLoggerProvider;
        this.existingHandler = existingHandler;
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        reportCrash(t, e);

        // do our best to make sure the crash makes it out of the VM
        CompletableResultCode flushResult = sdkLoggerProvider.forceFlush();
        flushResult.join(10, TimeUnit.SECONDS);

        // preserve any existing behavior
        if (existingHandler != null) {
            existingHandler.uncaughtException(t, e);
        }
    }

    private void reportCrash(Thread t, Throwable e) {
        CrashDetails crashDetails = CrashDetails.create(t, e);
        crashSender.accept(crashDetails);
    }
}
