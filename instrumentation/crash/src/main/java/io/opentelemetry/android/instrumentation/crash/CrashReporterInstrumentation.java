/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import android.app.Application;
import androidx.annotation.NonNull;
import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.android.RuntimeDetailsExtractor;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.ArrayList;
import java.util.List;

/** A builder of {@link CrashReporter}. */
public final class CrashReporterInstrumentation implements AndroidInstrumentation {

    final List<AttributesExtractor<CrashDetails, Void>> additionalExtractors = new ArrayList<>();

    public CrashReporterInstrumentation() {}

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public CrashReporterInstrumentation addAttributesExtractor(
            AttributesExtractor<CrashDetails, Void> extractor) {
        additionalExtractors.add(extractor);
        return this;
    }

    @Override
    public void apply(
            @NonNull Application application, @NonNull OpenTelemetryRum openTelemetryRum) {
        addAttributesExtractor(RuntimeDetailsExtractor.create(application));
        CrashReporter crashReporter = new CrashReporter(additionalExtractors);
        crashReporter.install(openTelemetryRum);
    }
}
