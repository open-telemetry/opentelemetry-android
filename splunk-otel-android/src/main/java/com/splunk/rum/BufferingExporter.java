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

package com.splunk.rum;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

class BufferingExporter implements SpanExporter {
    private static final int MAX_BACKLOG_SIZE = 100;

    private final SpanExporter delegate;
    //note: no need to make this queue thread-safe since it will only ever be called from the BatchSpanProcessor worker thread.
    private final Queue<SpanData> backlog = new ArrayDeque<>(MAX_BACKLOG_SIZE);

    BufferingExporter(SpanExporter delegate) {
        this.delegate = delegate;
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        backlog.addAll(spans);
        List<SpanData> toExport = fillFromBacklog();
        CompletableResultCode exportResult = delegate.export(toExport);
        exportResult.whenComplete(() -> {
            if (exportResult.isSuccess()) {
                return;
            }
            addFailedSpansToBacklog(toExport);
        });
        return exportResult;
    }

    //todo Should we favor saving certain kinds of span if we're out of space? Or favor recency?
    private void addFailedSpansToBacklog(List<SpanData> toExport) {
        for (SpanData spanData : toExport) {
            if (backlog.size() < MAX_BACKLOG_SIZE) {
                backlog.add(spanData);
            }
        }
    }

    @NonNull
    private List<SpanData> fillFromBacklog() {
        List<SpanData> retries = new ArrayList<>(backlog);
        backlog.clear();
        return retries;
    }

    @Override
    public CompletableResultCode flush() {
        if (!backlog.isEmpty()) {
            //note: the zipkin exporter has a no-op flush() method, so no need to call it after this.
            return export(fillFromBacklog());
        }
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        backlog.clear();
        return delegate.shutdown();
    }
}
