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
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.ArrayList;
import java.util.List;

/** Entrypoint for installing the crash reporting instrumentation. */
public final class CrashReporterInstrumentation implements AndroidInstrumentation {
    private final List<AttributesExtractor<CrashDetails, Void>> additionalExtractors =
            new ArrayList<>();

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public void addAttributesExtractor(AttributesExtractor<CrashDetails, Void> extractor) {
        additionalExtractors.add(extractor);
    }

    @Override
    public void install(
            @NonNull Application application, @NonNull OpenTelemetryRum openTelemetryRum) {
        addAttributesExtractor(RuntimeDetailsExtractor.create(application));
        CrashReporter crashReporter = new CrashReporter(additionalExtractors);
        crashReporter.install((OpenTelemetrySdk) openTelemetryRum.getOpenTelemetry());
    }
}
