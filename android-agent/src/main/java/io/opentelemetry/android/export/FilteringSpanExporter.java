/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** An exporter that will filter (not export) spans that fail a predicate. */
public class FilteringSpanExporter implements SpanExporter {

    private final SpanExporter delegate;

    private final Predicate<SpanData> spanRejecter;

    public static FilteringSpanExporterBuilder builder(SpanExporter delegate) {
        return new FilteringSpanExporterBuilder(delegate);
    }

    FilteringSpanExporter(SpanExporter delegate, Predicate<SpanData> spanRejecter) {
        this.delegate = delegate;
        this.spanRejecter = spanRejecter;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        List<SpanData> toExport =
                spans.stream().filter(spanRejecter.negate()).collect(Collectors.toList());
        return delegate.export(toExport);
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }
}
