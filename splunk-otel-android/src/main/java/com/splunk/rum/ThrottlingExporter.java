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

import android.util.Log;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

class ThrottlingExporter implements SpanExporter {
    private final SpanExporter delegate;
    private final Function<SpanData, String> categoryFunction;
    private final long windowSizeInNanos;
    private final int maxSpansInWindow;
    // note: no need to make this thread-safe since it will only ever be called from the BatchSpanProcessor worker thread.
    // the implementation here needs to support null keys, or we'd need to use a default component value.
    private final Map<String, Window> categoryToWindow = new HashMap<>();

    private ThrottlingExporter(Builder builder) {
        this.delegate = builder.delegate;
        this.categoryFunction = builder.categoryFunction;
        this.windowSizeInNanos = builder.windowSize.toNanos();
        this.maxSpansInWindow = builder.maxSpansInWindow;
    }

    static Builder newBuilder(SpanExporter delegate) {
        return new Builder(delegate);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        List<SpanData> spansBelowLimit = new ArrayList<>();
        for (SpanData span : spans) {
            String category = categoryFunction.apply(span);
            Window window = categoryToWindow.computeIfAbsent(category, k -> new Window());
            if (!window.aboveLimit(span)) {
                spansBelowLimit.add(span);
            }
        }
        int dropped = spans.size() - spansBelowLimit.size();
        if (dropped > 0) {
            Log.d(SplunkRum.LOG_TAG, "Dropped " + dropped + " spans because of throttling");
        }
        return delegate.export(spansBelowLimit);
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    class Window {
        private final Deque<Long> timestamps = new ArrayDeque<>();

        // this function assumes that spans are always sorted by their end time (ascending)
        boolean aboveLimit(SpanData spanData) {
            long endNanos = spanData.getEndEpochNanos();
            timestamps.addLast(endNanos);

            // remove oldest entries until the window shrinks to the configured size
            while (true) {
                Long first = timestamps.peekFirst();
                //this shouldn't happen, due to the single-threaded nature of things here, but
                //just to be on the safe side, don't blow up if something has cleared out the window.
                if (first == null) {
                    break;
                }
                if (endNanos - first < windowSizeInNanos) {
                    break;
                }
                timestamps.removeFirst();
            }

            boolean aboveLimit = timestamps.size() > maxSpansInWindow;
            // don't count spans that were throttled
            if (aboveLimit) {
                timestamps.removeLast();
            }
            return aboveLimit;
        }
    }

    static class Builder {
        final SpanExporter delegate;
        Function<SpanData, String> categoryFunction = span -> "default";
        Duration windowSize = Duration.ofSeconds(30);
        int maxSpansInWindow = 100;

        private Builder(SpanExporter delegate) {
            this.delegate = delegate;
        }

        Builder categorizeByAttribute(AttributeKey<String> attributeKey) {
            categoryFunction = spanData -> spanData.getAttributes().get(attributeKey);
            return this;
        }

        Builder windowSize(Duration timeWindow) {
            this.windowSize = timeWindow;
            return this;
        }

        Builder maxSpansInWindow(int maxSpansInWindow) {
            this.maxSpansInWindow = maxSpansInWindow;
            return this;
        }

        ThrottlingExporter build() {
            return new ThrottlingExporter(this);
        }
    }
}
