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

package io.opentelemetry.rum.internal.instrumentation.anr;

import android.os.Handler;
import android.os.Looper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.rum.internal.instrumentation.InstrumentedApplication;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/** Entrypoint for installing the ANR (application not responding) detection instrumentation. */
public final class AnrDetector {

    /** Returns a new {@link AnrDetector} with the default settings. */
    public static AnrDetector create() {
        return builder().build();
    }

    /** Returns a new {@link AnrDetectorBuilder}. */
    public static AnrDetectorBuilder builder() {
        return new AnrDetectorBuilder();
    }

    private final List<AttributesExtractor<StackTraceElement[], Void>> additionalExtractors;
    private final Looper mainLooper;
    private final ScheduledExecutorService scheduler;

    AnrDetector(AnrDetectorBuilder builder) {
        this.additionalExtractors = builder.additionalExtractors;
        this.mainLooper = builder.mainLooper;
        this.scheduler = builder.scheduler;
    }

    /**
     * Installs the ANR detection instrumentation on the given {@link InstrumentedApplication}.
     *
     * <p>When the main thread is unresponsive for 5 seconds or more, an event including the main
     * thread's stack trace will be reported to the RUM system.
     */
    public void installOn(InstrumentedApplication instrumentedApplication) {
        Handler uiHandler = new Handler(mainLooper);
        AnrWatcher anrWatcher =
                new AnrWatcher(
                        uiHandler,
                        mainLooper.getThread(),
                        buildAnrInstrumenter(instrumentedApplication.getOpenTelemetrySdk()));

        AnrDetectorToggler listener = new AnrDetectorToggler(anrWatcher, scheduler);
        // call it manually the first time to enable the ANR detection
        listener.onApplicationForegrounded();

        instrumentedApplication.registerApplicationStateListener(listener);
    }

    private Instrumenter<StackTraceElement[], Void> buildAnrInstrumenter(
            OpenTelemetry openTelemetry) {
        return Instrumenter.<StackTraceElement[], Void>builder(
                        openTelemetry, "io.opentelemetry.anr", stackTrace -> "ANR")
                // it's always an error
                .setSpanStatusExtractor(
                        (spanStatusBuilder, stackTrace, unused, error) ->
                                spanStatusBuilder.setStatus(StatusCode.ERROR))
                .addAttributesExtractor(new StackTraceFormatter())
                .addAttributesExtractors(additionalExtractors)
                .buildInstrumenter();
    }
}
