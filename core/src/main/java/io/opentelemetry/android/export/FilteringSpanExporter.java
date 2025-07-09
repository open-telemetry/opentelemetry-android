/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export;

import io.opentelemetry.sdk.trace.export.SpanExporter;

/** An exporter that will filter (not export) spans that fail a predicate. */
public class FilteringSpanExporter {

    public static FilteringSpanExporterBuilder builder(SpanExporter delegate) {
        return new FilteringSpanExporterBuilder(delegate);
    }
}
