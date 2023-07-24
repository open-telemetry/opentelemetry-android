/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal.instrumentation.crash;

import androidx.annotation.NonNull;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.concurrent.TimeUnit;

final class CrashReportingExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Instrumenter<CrashDetails, Void> instrumenter;
    private final SdkTracerProvider sdkTracerProvider;
    private final Thread.UncaughtExceptionHandler existingHandler;

    CrashReportingExceptionHandler(
            Instrumenter<CrashDetails, Void> instrumenter,
            SdkTracerProvider sdkTracerProvider,
            Thread.UncaughtExceptionHandler existingHandler) {
        this.instrumenter = instrumenter;
        this.sdkTracerProvider = sdkTracerProvider;
        this.existingHandler = existingHandler;
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        reportCrash(t, e);

        // do our best to make sure the crash makes it out of the VM
        CompletableResultCode flushResult = sdkTracerProvider.forceFlush();
        flushResult.join(10, TimeUnit.SECONDS);

        // preserve any existing behavior
        if (existingHandler != null) {
            existingHandler.uncaughtException(t, e);
        }
    }

    private void reportCrash(Thread t, Throwable e) {
        CrashDetails crashDetails = CrashDetails.create(t, e);
        Context context = instrumenter.start(Context.current(), crashDetails);
        instrumenter.end(context, crashDetails, null, e);
    }
}
