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
