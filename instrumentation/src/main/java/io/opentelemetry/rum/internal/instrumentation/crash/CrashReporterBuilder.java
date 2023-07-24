/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal.instrumentation.crash;

import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.util.ArrayList;
import java.util.List;

/** A builder of {@link CrashReporter}. */
public final class CrashReporterBuilder {

    CrashReporterBuilder() {}

    final List<AttributesExtractor<CrashDetails, Void>> additionalExtractors = new ArrayList<>();

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public CrashReporterBuilder addAttributesExtractor(
            AttributesExtractor<CrashDetails, Void> extractor) {
        additionalExtractors.add(extractor);
        return this;
    }

    /**
     * Returns a new {@link CrashReporter} with the settings of this {@link CrashReporterBuilder}.
     */
    public CrashReporter build() {
        return new CrashReporter(this);
    }
}
