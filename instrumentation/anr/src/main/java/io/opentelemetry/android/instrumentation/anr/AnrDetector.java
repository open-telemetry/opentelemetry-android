/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.android.internal.services.ServiceManager;
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleService;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/** Entrypoint for installing the ANR (application not responding) detection instrumentation. */
public final class AnrDetector {

    private final List<AttributesExtractor<StackTraceElement[], Void>> additionalExtractors;
    private final Looper mainLooper;
    private final ScheduledExecutorService scheduler;

    public AnrDetector(
            List<AttributesExtractor<StackTraceElement[], Void>> additionalExtractors,
            Looper mainLooper,
            ScheduledExecutorService scheduler) {
        this.additionalExtractors = additionalExtractors;
        this.mainLooper = mainLooper;
        this.scheduler = scheduler;
    }

    /**
     * Installs the ANR detection instrumentation.
     *
     * <p>When the main thread is unresponsive for 5 seconds or more, an event including the main
     * thread's stack trace will be reported to the RUM system.
     */
    public void install(@NonNull OpenTelemetryRum openTelemetryRum) {
        Handler uiHandler = new Handler(mainLooper);
        AnrWatcher anrWatcher =
                new AnrWatcher(
                        uiHandler,
                        mainLooper.getThread(),
                        buildAnrInstrumenter(openTelemetryRum.getOpenTelemetry()));

        AnrDetectorToggler listener = new AnrDetectorToggler(anrWatcher, scheduler);
        // call it manually the first time to enable the ANR detection
        listener.onApplicationForegrounded();

        ServiceManager.get().getService(AppLifecycleService.class).registerListener(listener);
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
