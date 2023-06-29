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

package io.opentelemetry.rum.internal.export;

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
