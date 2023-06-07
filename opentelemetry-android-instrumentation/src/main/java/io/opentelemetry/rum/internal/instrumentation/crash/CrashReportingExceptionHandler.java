/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
