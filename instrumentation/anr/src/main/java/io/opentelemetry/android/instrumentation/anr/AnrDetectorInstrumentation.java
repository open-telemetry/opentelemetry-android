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

    final List<AttributesExtractor<StackTraceElement[], Void>> additionalExtractors =
            new ArrayList<>();
    Looper mainLooper = Looper.getMainLooper();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Override
    public void apply(
            @NonNull Application application, @NonNull OpenTelemetryRum openTelemetryRum) {
        AnrDetector anrDetector = new AnrDetector(additionalExtractors, mainLooper, scheduler);
        anrDetector.install(openTelemetryRum);
    }
}
