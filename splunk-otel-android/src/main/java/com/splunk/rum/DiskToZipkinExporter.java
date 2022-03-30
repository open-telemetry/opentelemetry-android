package com.splunk.rum;

import static com.splunk.rum.SplunkRum.LOG_TAG;
import static java.util.Collections.emptyList;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import zipkin2.Call;
import zipkin2.reporter.Sender;

/**
 * An exporter that pulls pre-encoded zipkin spans from storage and sends them
 * via a sender. It is bandwidth sensitive and will throttle back if the limit
 * is exceeded.
 */
class DiskToZipkinExporter {

    static final double DEFAULT_MAX_UNCOMPRESSED_BANDWIDTH = 15.0 * 1024;

    private final ScheduledExecutorService threadPool;
    private final ConnectionUtil connectionUtil;
    private final FileSender fileSender;
    private final File spanFilesPath;
    private final FileUtils fileUtils;
    private final BandwidthTracker bandwidthTracker;
    private final double bandwidthLimit;

    DiskToZipkinExporter(Builder builder) {
        this.threadPool = builder.threadPool;
        this.connectionUtil = builder.connectionUtil;
        this.fileSender = builder.fileSender;
        this.spanFilesPath = builder.spanFilesPath;
        this.fileUtils = builder.fileUtils;
        this.bandwidthTracker = builder.bandwidthTracker;
        this.bandwidthLimit = builder.bandwidthLimit;
    }

    void startPolling() {
        threadPool.scheduleAtFixedRate(this::doExportCycle, 5, 5, TimeUnit.SECONDS);
    }

    //Visible for testing
    void doExportCycle() {
        try {
            exportPendingFiles();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error processing on-disk files", e);
        }
    }

    private void exportPendingFiles() {
        if (!connectionUtil.refreshNetworkStatus().isOnline()) {
            Log.i(SplunkRum.LOG_TAG, "Network offline, leaving spans on disk for for eventual export.");
            return;
        }

        List<File> pendingFiles = getPendingFiles();
        boolean sentAnything = false;
        for (File file : pendingFiles) {

            double sustainedRate = bandwidthTracker.totalSustainedRate();
            if (sustainedRate > bandwidthLimit) {
                Log.i(SplunkRum.LOG_TAG, String.format("Export rate %.2f exceeds limit of %.2f, backing off", sustainedRate, bandwidthLimit));
                break;
            }

            boolean dataWasSent = fileSender.handleFileOnDisk(file);
            sentAnything |= dataWasSent;
            if (!dataWasSent) {   // Don't bother trying any remaining files if this one failed.
                break;
            }
        }
        if (!sentAnything) {
            bandwidthTracker.tick(emptyList());
        }
    }

    private List<File> getPendingFiles() {
        return fileUtils.listFiles(spanFilesPath)
                .filter(fileUtils::isRegularFile)
                .filter(file -> file.toString().endsWith(".spans"))
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
        private FileSender fileSender;
        private BandwidthTracker bandwidthTracker;
        private ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor();
        private ConnectionUtil connectionUtil;
        private File spanFilesPath;
        private FileUtils fileUtils = new FileUtils();
        private double bandwidthLimit = DEFAULT_MAX_UNCOMPRESSED_BANDWIDTH;

        Builder threadPool(ScheduledExecutorService threadPool) {
            this.threadPool = threadPool;
            return this;
        }

        Builder connectionUtil(ConnectionUtil connectionUtil) {
            this.connectionUtil = connectionUtil;
            return this;
        }

        Builder bandwidthTracker(BandwidthTracker bandwidthTracker){
            this.bandwidthTracker = bandwidthTracker;
            return this;
        }

        Builder fileSender(FileSender fileSender){
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
