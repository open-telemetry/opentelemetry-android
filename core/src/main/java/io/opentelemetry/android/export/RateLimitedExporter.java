/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.opentelemetry.android;

import android.util.Log;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

class BandwidthThrottlingExporter implements SpanExporter {
    private final SpanExporter delegate;
    private final Function<SpanData, String> categoryFunction;
    private final long maxBytesPerSecond;
    private final long timeWindowInMillis;
    private long lastExportTime;
    private long bytesExportedInWindow;

    private BandwidthThrottlingExporter(Builder builder) {
        this.delegate = builder.delegate;
        this.categoryFunction = builder.categoryFunction;
        this.maxBytesPerSecond = builder.maxBytesPerSecond;
        this.timeWindowInMillis = builder.timeWindow.toMillis();
        this.lastExportTime = System.currentTimeMillis();

        this.bytesExportedInWindow = 0;
    }

    static Builder newBuilder(SpanExporter delegate) {
        return new Builder(delegate);
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        List<SpanData> spansToExport = new ArrayList<>();
        long totalBytes = 0;

        for (SpanData span : spans) {
            // Estimate the size of the span (this can be adjusted based on actual size)
            long spanSize = estimateSpanSize(span);
            totalBytes += spanSize;

            // Check if we can export this span based on the current bandwidth limit
            if (canExport(spanSize)) {
                spansToExport.add(span);
                bytesExportedInWindow += spanSize;
            } else {
                Log.d("BandwidthThrottlingExporter", "Throttled span: " + span.getName());
            }
        }

        return delegate.export(spansToExport);
    }

    private boolean canExport(long spanSize) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastExportTime > timeWindowInMillis) {
            // Reset the window
            bytesExportedInWindow = 0;
            lastExportTime = currentTime;
        }

        return (bytesExportedInWindow + spanSize)
                <= maxBytesPerSecond * (timeWindowInMillis / 1000);
    }

    private long estimateSpanSize(SpanData span) {
        // This is a placeholder for actual size estimation logic
        return span.getAttributes().size() * 8; // Example: 8 bytes per attribute
    }

    @Override
    public CompletableResultCode flush() {
        return delegate.flush();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    static class Builder {
        final SpanExporter delegate;
        Function<SpanData, String> categoryFunction = span -> "default";
        long maxBytesPerSecond = 1024; // Default to 1 KB/s
        Duration timeWindow = Duration.ofSeconds(1); // Default to 1 second

        private Builder(SpanExporter delegate) {
            this.delegate = delegate;
        }

        Builder maxBytesPerSecond(long maxBytesPerSecond) {
            this.maxBytesPerSecond = maxBytesPerSecond;
            return this;
        }

        Builder timeWindow(Duration timeWindow) {
            this.timeWindow = timeWindow;
            return this;
        }

        BandwidthThrottlingExporter build() {
            return new BandwidthThrottlingExporter(this);
        }
    }
}
