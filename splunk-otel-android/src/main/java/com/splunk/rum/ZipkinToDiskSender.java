package com.splunk.rum;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.List;

import zipkin2.Call;
import zipkin2.codec.Encoding;
import zipkin2.reporter.Sender;

class ZipkinToDiskSender extends Sender {

    private final File path;
    private final FileUtils fileUtils;
    private final Clock clock;
    private final DeviceSpanStorageLimiter storageLimiter;

    private ZipkinToDiskSender(Builder builder) {
        this.path = builder.path;
        this.fileUtils = builder.fileUtils;
        this.clock = builder.clock;
        this.storageLimiter = builder.storageLimiter;
    }

    @Override
    public Encoding encoding() {
        return Encoding.JSON;
    }

    @Override
    public int messageMaxBytes() {
        return 1024 * 1024;
    }

    @Override
    public int messageSizeInBytes(List<byte[]> encodedSpans) {
        return encodedSpans.stream().reduce(0, (acc, cur) -> acc + cur.length + 1, Integer::sum);
    }

    @Override
    public Call<Void> sendSpans(List<byte[]> encodedSpans) {
        if (!storageLimiter.ensureFreeSpace()) {
            Log.e(SplunkRum.LOG_TAG, "Dropping " + encodedSpans.size() + " spans: Too much telemetry has been buffered or not enough space on device.");
            return Call.create(null);
        }
        long now = clock.millis();
        File filename = createFilename(now);
        try {
            fileUtils.writeAsLines(filename, encodedSpans);
        } catch (IOException e) {
            Log.e(SplunkRum.LOG_TAG, "Error writing spans to storage", e);
        }
        return Call.create(null);
    }

    private File createFilename(long now) {
        return new File(path, now + ".spans");
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private File path;
        private FileUtils fileUtils = new FileUtils();
        private Clock clock = Clock.systemDefaultZone();
        private DeviceSpanStorageLimiter storageLimiter;

        Builder path(File path){
            this.path = path;
            return this;
        }

        Builder fileUtils(FileUtils fileUtils){
            this.fileUtils = fileUtils;
            return this;
        }

        Builder clock(Clock clock){
            this.clock = clock;
            return this;
        }

        Builder storageLimiter(DeviceSpanStorageLimiter limiter){
            this.storageLimiter = limiter;
            return this;
        }

        ZipkinToDiskSender build() {
            return new ZipkinToDiskSender(this);
        }
    }
}
