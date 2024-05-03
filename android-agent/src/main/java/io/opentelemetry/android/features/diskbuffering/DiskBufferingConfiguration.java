/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering;

import io.opentelemetry.android.features.diskbuffering.scheduler.DefaultExportScheduleHandler;
import io.opentelemetry.android.features.diskbuffering.scheduler.DefaultExportScheduler;
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler;

/** Configuration for disk buffering. */
public final class DiskBufferingConfiguration {
    private final boolean enabled;
    private final int maxCacheSize;
    private final ExportScheduleHandler exportScheduleHandler;
    private static final int DEFAULT_MAX_CACHE_SIZE = 60 * 1024 * 1024;
    private static final int MAX_FILE_SIZE = 1024 * 1024;

    private DiskBufferingConfiguration(Builder builder) {
        this.enabled = builder.enabled;
        this.maxCacheSize = builder.maxCacheSize;
        this.exportScheduleHandler = builder.exportScheduleHandler;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public int getMaxCacheFileSize() {
        return MAX_FILE_SIZE;
    }

    public ExportScheduleHandler getExportScheduleHandler() {
        return exportScheduleHandler;
    }

    public static final class Builder {
        private boolean enabled = false;
        private int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;
        private ExportScheduleHandler exportScheduleHandler =
                new DefaultExportScheduleHandler(new DefaultExportScheduler());


        /** Enables or disables disk buffering. */
        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets the maximum amount of bytes that this tool can use to store cached signals in disk.
         * A smaller amount of space will be used if there's not enough space in disk to allocate
         * the value provided here.
         */
        public Builder setMaxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        /**
         * Sets a scheduler that will take care of periodically read data stored in disk and export
         * it.
         */
        public Builder setExportScheduleHandler(ExportScheduleHandler exportScheduleHandler) {
            this.exportScheduleHandler = exportScheduleHandler;
            return this;
        }

        public DiskBufferingConfiguration build() {
            return new DiskBufferingConfiguration(this);
        }
    }
}
