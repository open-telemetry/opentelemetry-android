package com.splunk.rum;

import android.app.Application;
import android.util.Log;

import java.io.File;
import java.util.Locale;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import zipkin2.reporter.Sender;

/**
 * Creates a ZipkinSpanExporter that is configured with an instance of
 * a ZipkinToDiskSender that writes telemetry to disk.
 */
class ZipkinWriteToDiskExporterFactory {

    private ZipkinWriteToDiskExporterFactory(){
    }

    static ZipkinSpanExporter create(Application application, Config config) {
        File spansPath = FileUtils.getSpansDirectory(application);
        if (!spansPath.exists()) {
            if(!spansPath.mkdirs()){
                Log.e(SplunkRum.LOG_TAG, "Error creating path " + spansPath + " for span buffer, defaulting to parent");
                spansPath = application.getApplicationContext().getFilesDir();
            }
        }

        FileUtils fileUtils = new FileUtils();
        DeviceSpanStorageLimiter limiter = DeviceSpanStorageLimiter.builder()
                .fileUtils(fileUtils)
                .path(spansPath)
                .maxStorageUseMb(config.getMaxUsageMegabytes())
                .build();
        Sender sender = ZipkinToDiskSender.builder()
                .path(spansPath)
                .fileUtils(fileUtils)
                .storageLimiter(limiter)
                .build();
        return ZipkinSpanExporter.builder()
                .setEncoder(new CustomZipkinEncoder())
                .setSender(sender)
                .build();
    }
}
