package com.splunk.rum;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;

import zipkin2.Call;
import zipkin2.codec.Encoding;
import zipkin2.reporter.Sender;

class ZipkinToDiskSender extends Sender {

    private final File path;
    private final FileUtils fileUtils;
    private final Clock clock;

    ZipkinToDiskSender(File path) {
        this(path, new FileUtils(), Clock.systemDefaultZone());
    }

    // exists for testing
    ZipkinToDiskSender(File path, FileUtils fileUtils, Clock clock) {
        this.path = path;
        this.fileUtils = fileUtils;
        this.clock = clock;
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
}
