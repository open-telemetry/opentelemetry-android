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

import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import zipkin2.Call;
import zipkin2.reporter.Sender;

class FileSender {

    private static final int DEFAULT_MAX_RETRIES = 20;

    private final Sender sender;
    private final FileUtils fileUtils;
    private final BandwidthTracker bandwidthTracker;
    private final RetryTracker retryTracker;

    private FileSender(Builder builder) {
        this.sender = builder.sender;
        this.fileUtils = builder.fileUtils;
        this.bandwidthTracker = builder.bandwidthTracker;
        this.retryTracker = builder.retryTracker;
    }

    /**
     * Reads a file on disk and attempts to send it. Updates the bandwidthTracker with the bytes
     * read, and return true if the file was sent. It will keep track of how many attempts the file
     * has had, and if it exceedes the max retries, the file will be deleted.
     *
     * @param file File to handle
     * @return true if the file content was sent successfully
     */
    boolean handleFileOnDisk(File file) {
        Log.d(LOG_TAG, "Reading file content for ingest: " + file);
        List<byte[]> encodedSpans = readFileCompletely(file);
        if (encodedSpans.isEmpty()) {
            return false;
        }

        boolean sentOk = attemptSend(file, encodedSpans);
        if (!sentOk) {
            retryTracker.trackFailure(file);
        }
        if (sentOk || retryTracker.exceededRetries(file)) {
            retryTracker.clear(file);
            fileUtils.safeDelete(file);
        }
        return sentOk;
    }

    private boolean attemptSend(File file, List<byte[]> encodedSpans) {
        try {
            bandwidthTracker.tick(encodedSpans);
            Call<Void> httpCall = sender.sendSpans(encodedSpans);
            httpCall.execute();
            Log.d(LOG_TAG, "File content " + file + " successfully uploaded");
            return true;
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error sending file content", e);
            return false;
        }
    }

    private List<byte[]> readFileCompletely(File file) {
        try {
            return fileUtils.readFileCompletely(file);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error reading span data from file " + file, e);
            return emptyList();
        }
    }

    static Builder builder() {
        return new Builder();
    }

    private static class RetryTracker {
        private final Map<File, Integer> attempts = new HashMap<>();
        private final int maxRetries;
        private final Consumer<Integer> backoff;

        private RetryTracker(int maxRetries, Consumer<Integer> backoff) {
            this.maxRetries = maxRetries;
            this.backoff = backoff;
        }

        void clear(File file) {
            attempts.remove(file);
        }

        /**
         * Updates the count of tracked failures for this file. If the retry count has not been
         * exceeded, it will perform a backoff step.
         *
         * @param file - the file for which an attempt was unsuccessful
         */
        void trackFailure(File file) {
            Integer retryCount = attempts.merge(file, 1, (cur, x) -> cur + 1);
            boolean exceededRetries = retryCount >= maxRetries;
            if (exceededRetries) {
                Log.w(
                        LOG_TAG,
                        "Dropping data in " + file + " (max retries exceeded " + maxRetries + ")");
            } else {
                backoff.accept(retryCount);
            }
        }

        boolean exceededRetries(File file) {
            return attempts.getOrDefault(file, 0) >= maxRetries;
        }
    }

    static class DefaultBackoff implements Consumer<Integer> {

        @Override
        public void accept(Integer attempts) {
            long seconds = Math.min(60, attempts * 5);
            try {
                TimeUnit.SECONDS.sleep(seconds);
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "Error during backoff", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    static class Builder {

        private Sender sender;
        private FileUtils fileUtils = new FileUtils();
        private BandwidthTracker bandwidthTracker;
        private RetryTracker retryTracker;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private Consumer<Integer> backoff = new DefaultBackoff();

        Builder sender(Sender sender) {
            this.sender = sender;
            return this;
        }

        Builder fileUtils(FileUtils fileUtils) {
            this.fileUtils = fileUtils;
            return this;
        }

        Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        Builder bandwidthTracker(BandwidthTracker bandwidthTracker) {
            this.bandwidthTracker = bandwidthTracker;
            return this;
        }

        // Exists for testing
        Builder backoff(Consumer<Integer> backoff) {
            this.backoff = backoff;
            return this;
        }

        FileSender build() {
            this.retryTracker = new RetryTracker(maxRetries, backoff);
            return new FileSender(this);
        }
    }
}
