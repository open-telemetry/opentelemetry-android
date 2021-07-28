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

package com.splunk.rum;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

class CrashReporter {

    static void initializeCrashReporting(Tracer tracer, OpenTelemetrySdk openTelemetrySdk) {
        Thread.UncaughtExceptionHandler existingHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new CrashReportingExceptionHandler(tracer, openTelemetrySdk.getSdkTracerProvider(), existingHandler));
    }

    //visible for testing
    static class CrashReportingExceptionHandler implements Thread.UncaughtExceptionHandler {
        private final Tracer tracer;
        private final Thread.UncaughtExceptionHandler existingHandler;
        private final SdkTracerProvider sdkTracerProvider;

        CrashReportingExceptionHandler(Tracer tracer, SdkTracerProvider sdkTracerProvider, Thread.UncaughtExceptionHandler existingHandler) {
            this.tracer = tracer;
            this.existingHandler = existingHandler;
            this.sdkTracerProvider = sdkTracerProvider;
        }

        @Override
        public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            String exceptionType = e.getClass().getSimpleName();
            tracer.spanBuilder(exceptionType)
                    .setAttribute(SemanticAttributes.THREAD_ID, t.getId())
                    .setAttribute(SemanticAttributes.THREAD_NAME, t.getName())
                    .setAttribute(SemanticAttributes.EXCEPTION_STACKTRACE, writer.toString())
                    .setAttribute(SemanticAttributes.EXCEPTION_ESCAPED, true)
                    .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_CRASH)
                    .startSpan()
                    .setStatus(StatusCode.ERROR)
                    .end();
            //do our best to make sure the crash makes it out of the VM
            sdkTracerProvider.forceFlush();
            //preserve any existing behavior:
            if (existingHandler != null) {
                existingHandler.uncaughtException(t, e);
            }
        }
    }
}
