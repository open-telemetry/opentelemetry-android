/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal.instrumentation.anr;

import android.os.Looper;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** A builder of {@link AnrDetector}. */
public final class AnrDetectorBuilder {

    AnrDetectorBuilder() {}

    final List<AttributesExtractor<StackTraceElement[], Void>> additionalExtractors =
            new ArrayList<>();
    Looper mainLooper = Looper.getMainLooper();
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public AnrDetectorBuilder addAttributesExtractor(
            AttributesExtractor<StackTraceElement[], Void> extractor) {
        additionalExtractors.add(extractor);
        return this;
    }

    /** Sets a custom {@link Looper} to run on. Useful for testing. */
    public AnrDetectorBuilder setMainLooper(Looper looper) {
        mainLooper = looper;
        return this;
    }

    // visible for tests
    AnrDetectorBuilder setScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    /** Returns a new {@link AnrDetector} with the settings of this {@link AnrDetectorBuilder}. */
    public AnrDetector build() {
        return new AnrDetector(this);
    }
}
