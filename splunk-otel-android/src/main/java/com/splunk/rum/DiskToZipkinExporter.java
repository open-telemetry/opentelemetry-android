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

import static com.splunk.rum.SplunkRum.LOG_TAG;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import android.util.Log;
import androidx.annotation.Nullable;
import io.opentelemetry.rum.internal.instrumentation.network.CurrentNetworkProvider;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * An exporter that pulls pre-encoded zipkin spans from storage and sends them via a sender. It is
 * bandwidth sensitive and will throttle back if the limit is exceeded.
 */
class DiskToZipkinExporter {

    static final double DEFAULT_MAX_UNCOMPRESSED_BANDWIDTH = 15.0 * 1024;

    private final ScheduledExecutorService threadPool;
    private final CurrentNetworkProvider currentNetworkProvider;
    private final FileSender fileSender;
    private final File spanFilesPath;
    private final FileUtils fileUtils;
    private final BandwidthTracker bandwidthTracker;
    private final double bandwidthLimit;

    DiskToZipkinExporter(Builder builder) {
        this.threadPool = builder.threadPool;
        this.currentNetworkProvider = requireNonNull(builder.currentNetworkProvider);
        this.fileSender = requireNonNull(builder.fileSender);
        this.spanFilesPath = requireNonNull(builder.spanFilesPath);
        this.fileUtils = builder.fileUtils;
        this.bandwidthTracker = requireNonNull(builder.bandwidthTracker);
        this.bandwidthLimit = builder.bandwidthLimit;
    }

    // the returned future is very unlikely to fail
    @SuppressWarnings("FutureReturnValueIgnored")
    void startPolling() {
        threadPool.scheduleAtFixedRate(this::doExportCycle, 5, 5, TimeUnit.SECONDS);
    }

    // Visible for testing
    void doExportCycle() {
        try {
            exportPendingFiles();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error processing on-disk files", e);
        }
    }

    private void exportPendingFiles() {
        if (!currentNetworkProvider.refreshNetworkStatus().isOnline()) {
            Log.i(
                    SplunkRum.LOG_TAG,
                    "Network offline, leaving spans on disk for for eventual export.");
            return;
        }

        List<File> pendingFiles = getPendingFiles();
        boolean sentAnything = false;
        for (File file : pendingFiles) {

            double sustainedRate = bandwidthTracker.totalSustainedRate();
            if (sustainedRate > bandwidthLimit) {
                Log.i(
                        SplunkRum.LOG_TAG,
                        String.format(
                                "Export rate %.2f exceeds limit of %.2f, backing off",
                                sustainedRate, bandwidthLimit));
                break;
            }

            boolean dataWasSent = fileSender.handleFileOnDisk(file);
            sentAnything |= dataWasSent;
            if (!dataWasSent) { // Don't bother trying any remaining files if this one failed.
                break;
            }
        }
        if (!sentAnything) {
            bandwidthTracker.tick(emptyList());
        }
    }

    private List<File> getPendingFiles() {
        return fileUtils
                .listSpanFiles(spanFilesPath)
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());
    }

    void stop() {
        threadPool.shutdown();
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        @Nullable private FileSender fileSender;
        @Nullable private BandwidthTracker bandwidthTracker;
        private ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();
        @Nullable private CurrentNetworkProvider currentNetworkProvider;
        @Nullable private File spanFilesPath;
        private FileUtils fileUtils = new FileUtils();
        private double bandwidthLimit = DEFAULT_MAX_UNCOMPRESSED_BANDWIDTH;

        Builder threadPool(ScheduledExecutorService threadPool) {
            this.threadPool = threadPool;
            return this;
        }

        Builder connectionUtil(CurrentNetworkProvider currentNetworkProvider) {
            this.currentNetworkProvider = currentNetworkProvider;
            return this;
        }

        Builder bandwidthTracker(BandwidthTracker bandwidthTracker) {
            this.bandwidthTracker = bandwidthTracker;
            return this;
        }

        Builder fileSender(FileSender fileSender) {
            this.fileSender = fileSender;
            return this;
        }

        Builder bandwidthLimit(double limit) {
            this.bandwidthLimit = limit;
            return this;
        }

        Builder spanFilesPath(File spanFilesPath) {
            this.spanFilesPath = spanFilesPath;
            return this;
        }

        Builder fileUtils(FileUtils fileUtils) {
            this.fileUtils = fileUtils;
            return this;
        }

        DiskToZipkinExporter build() {
            return new DiskToZipkinExporter(this);
        }
    }
}
