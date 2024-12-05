/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import androidx.annotation.NonNull;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.common.RuntimeDetailsExtractor;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.ArrayList;
import java.util.List;

/** Entrypoint for installing the crash reporting instrumentation. */
@AutoService(AndroidInstrumentation.class)
public final class CrashReporterInstrumentation implements AndroidInstrumentation {
    private final List<AttributesExtractor<CrashDetails, Void>> additionalExtractors =
            new ArrayList<>();

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public void addAttributesExtractor(AttributesExtractor<CrashDetails, Void> extractor) {
        additionalExtractors.add(extractor);
    }

    @Override
    public void install(@NonNull InstallationContext ctx) {
        addAttributesExtractor(RuntimeDetailsExtractor.create(ctx.getApplication()));
        CrashReporter crashReporter = new CrashReporter(additionalExtractors);
        crashReporter.install((OpenTelemetrySdk) ctx.getOpenTelemetry());
    }
}
