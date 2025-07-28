/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import androidx.annotation.NonNull;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.ArrayList;
import java.util.List;

/** Entrypoint for installing the crash reporting instrumentation. */
@AutoService(AndroidInstrumentation.class)
public final class CrashReporterInstrumentation implements AndroidInstrumentation {
    private static final String INSTRUMENTATION_NAME = "crash";
    private final List<EventAttributesExtractor<CrashDetails>> additionalExtractors =
            new ArrayList<>();

    /** Adds an {@link EventAttributesExtractor} that will extract additional attributes. */
    public void addAttributesExtractor(EventAttributesExtractor<CrashDetails> extractor) {
        additionalExtractors.add(extractor);
    }

    @Override
    public void install(@NonNull InstallationContext ctx) {
        addAttributesExtractor(RuntimeDetailsExtractor.create(ctx.getApplication()));
        CrashReporter crashReporter = new CrashReporter(additionalExtractors);
        crashReporter.install((OpenTelemetrySdk) ctx.getOpenTelemetry());
    }

    @NonNull
    @Override
    public String getName() {
        return INSTRUMENTATION_NAME;
    }
}
