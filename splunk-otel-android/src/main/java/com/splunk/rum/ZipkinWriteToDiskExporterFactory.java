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

import android.app.Application;
import android.util.Log;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;

import zipkin2.reporter.Sender;

import java.io.File;

/**
 * Creates a ZipkinSpanExporter that is configured with an instance of a ZipkinToDiskSender that
 * writes telemetry to disk.
 */
class ZipkinWriteToDiskExporterFactory {

    private ZipkinWriteToDiskExporterFactory() {}

    static ZipkinSpanExporter create(Application application, int maxUsageMegabytes) {
        File spansPath = FileUtils.getSpansDirectory(application);
        if (!spansPath.exists()) {
            if (!spansPath.mkdirs()) {
                Log.e(
                        SplunkRum.LOG_TAG,
                        "Error creating path "
                                + spansPath
                                + " for span buffer, defaulting to parent");
                spansPath = application.getApplicationContext().getFilesDir();
            }
        }

        FileUtils fileUtils = new FileUtils();
        DeviceSpanStorageLimiter limiter =
                DeviceSpanStorageLimiter.builder()
                        .fileUtils(fileUtils)
                        .path(spansPath)
                        .maxStorageUseMb(maxUsageMegabytes)
                        .build();
        Sender sender =
                ZipkinToDiskSender.builder()
                        .path(spansPath)
                        .fileUtils(fileUtils)
                        .storageLimiter(limiter)
                        .build();
        return ZipkinSpanExporter.builder()
                .setEncoder(new CustomZipkinEncoder())
                .setSender(sender)
                // remove the local IP address
                .setLocalIpAddressSupplier(() -> null)
                .build();
    }
}
