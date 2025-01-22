/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import android.os.Looper;
import androidx.annotation.NonNull;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.android.internal.services.Services;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** Entry point for {@link AnrDetector}. */
@AutoService(AndroidInstrumentation.class)
public final class AnrInstrumentation implements AndroidInstrumentation {

    final List<AttributesExtractor<StackTraceElement[], Void>> additionalExtractors =
            new ArrayList<>();
    Looper mainLooper = Looper.getMainLooper();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public AnrInstrumentation addAttributesExtractor(
            AttributesExtractor<StackTraceElement[], Void> extractor) {
        additionalExtractors.add(extractor);
        return this;
    }

    /** Sets a custom {@link Looper} to run on. Useful for testing. */
    public AnrInstrumentation setMainLooper(Looper looper) {
        mainLooper = looper;
        return this;
    }

    // visible for tests
    AnrInstrumentation setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    @Override
    public void install(@NonNull InstallationContext ctx) {
        AnrDetector anrDetector =
                new AnrDetector(
                        additionalExtractors,
                        mainLooper,
                        scheduler,
                        Services.get(ctx.getApplication()).getAppLifecycle(),
                        ctx.getOpenTelemetry());
        anrDetector.start();
    }
}
