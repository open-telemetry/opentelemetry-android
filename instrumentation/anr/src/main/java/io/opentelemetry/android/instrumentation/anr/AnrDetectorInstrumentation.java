/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import android.app.Application;
import android.os.Looper;
import androidx.annotation.NonNull;
import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public final class AnrDetectorInstrumentation implements AndroidInstrumentation {

    private final List<AttributesExtractor<StackTraceElement[], Void>> additionalExtractors =
            new ArrayList<>();

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public void addAttributesExtractor(AttributesExtractor<StackTraceElement[], Void> extractor) {
        additionalExtractors.add(extractor);
    }

    @Override
    public void apply(
            @NonNull Application application, @NonNull OpenTelemetryRum openTelemetryRum) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AnrDetector anrDetector =
                new AnrDetector(additionalExtractors, Looper.getMainLooper(), scheduler);
        anrDetector.install(openTelemetryRum);
    }
}
