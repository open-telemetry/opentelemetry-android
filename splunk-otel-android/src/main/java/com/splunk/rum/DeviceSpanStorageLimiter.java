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

import static java.util.Comparator.comparingLong;
import static java.util.Objects.requireNonNull;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

class DeviceSpanStorageLimiter {
    static final int DEFAULT_MAX_STORAGE_USE_MB = 25;
    private final File path;
    private final int maxStorageUseMb;
    private final FileUtils fileUtils;

    private DeviceSpanStorageLimiter(Builder builder) {
        this.path = requireNonNull(builder.path);
        this.maxStorageUseMb = builder.maxStorageUseMb;
        this.fileUtils = builder.fileUtils;
    }

    /**
     * Ensures that the storage currently used by spans has not exceeded the limit. If it does, it
     * will delete older files until the limit is no longer exceeded.
     *
     * <p>This method also looks at the free space on the device and will return false if the
     * available free space is less than our max storage.
     *
     * @return - true if the free space is under the limit (including when files have been deleted
     *     to return back under the limit), false if not enough space could be freed to get us back
     *     under out limit.
     */
    boolean ensureFreeSpace() {
        tryFreeingSpace();
        // play nice if disk is getting full
        return path.getFreeSpace() > limitInBytes();
    }

    private void tryFreeingSpace() {
        long currentUsageInBytes = fileUtils.getTotalFileSizeInBytes(path);
        if (underLimit(currentUsageInBytes)) {
            return; // nothing to do
        }
        List<File> files =
                fileUtils
                        .listSpanFiles(path)
                        .sorted(comparingLong(fileUtils::getModificationTime))
                        .collect(Collectors.toList());
        for (File file : files) {
            Log.w(LOG_TAG, "Too much data buffered, dropping file " + file);
            long fileSize = fileUtils.getFileSize(file);
            fileUtils.safeDelete(file);
            currentUsageInBytes -= fileSize;
            if (underLimit(currentUsageInBytes)) {
                return;
            }
        }
    }

    private boolean underLimit(long currentUsageInBytes) {
        return currentUsageInBytes < limitInBytes();
    }

    private long limitInBytes() {
        return maxStorageUseMb * 1024L * 1024L;
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        @Nullable private File path;
        private int maxStorageUseMb = DEFAULT_MAX_STORAGE_USE_MB;
        private FileUtils fileUtils = new FileUtils();

        Builder path(File path) {
            this.path = path;
            return this;
        }

        Builder maxStorageUseMb(int maxStorageUseMb) {
            this.maxStorageUseMb = maxStorageUseMb;
            return this;
        }

        Builder fileUtils(FileUtils fileUtils) {
            this.fileUtils = fileUtils;
            return this;
        }

        DeviceSpanStorageLimiter build() {
            return new DeviceSpanStorageLimiter(this);
        }
    }
}
