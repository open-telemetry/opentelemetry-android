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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.rum.internal.instrumentation.InstrumentedApplication;
import java.util.List;

/** Entrypoint for installing the crash reporting instrumentation. */
public final class CrashReporter {

    /** Returns a new {@link CrashReporter} with the default settings. */
    public static CrashReporter create() {
        return builder().build();
    }

    /** Returns a new {@link CrashReporterBuilder}. */
    public static CrashReporterBuilder builder() {
        return new CrashReporterBuilder();
    }

    private final List<AttributesExtractor<CrashDetails, Void>> additionalExtractors;

    CrashReporter(CrashReporterBuilder builder) {
        this.additionalExtractors = builder.additionalExtractors;
    }

    /**
     * Installs the crash reporting instrumentation on the given {@link InstrumentedApplication}.
     */
    public void installOn(InstrumentedApplication instrumentedApplication) {
        Thread.UncaughtExceptionHandler existingHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(
                new CrashReportingExceptionHandler(
                        buildInstrumenter(instrumentedApplication.getOpenTelemetrySdk()),
                        instrumentedApplication.getOpenTelemetrySdk().getSdkTracerProvider(),
                        existingHandler));
    }

    private Instrumenter<CrashDetails, Void> buildInstrumenter(OpenTelemetry openTelemetry) {
        return Instrumenter.<CrashDetails, Void>builder(
                        openTelemetry, "io.opentelemetry.crash", CrashDetails::spanName)
                .addAttributesExtractor(new CrashDetailsAttributesExtractor())
                .addAttributesExtractors(additionalExtractors)
                .buildInstrumenter();
    }
}
