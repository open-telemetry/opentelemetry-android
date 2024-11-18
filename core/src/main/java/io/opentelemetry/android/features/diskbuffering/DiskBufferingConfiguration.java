/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Configuration for disk buffering. */
public final class DiskBufferingConfiguration {
    private static final int DEFAULT_MAX_CACHE_SIZE = 60 * 1024 * 1024;
    private static final int MAX_FILE_SIZE = 1024 * 1024;

    private final boolean enabled;
    private final int maxCacheSize;
    private final long maxFileAgeForWriteMillis;
    private final long minFileAgeForReadMillis;
    private final long maxFileAgeForReadMillis;

    private DiskBufferingConfiguration(Builder builder) {
        enabled = builder.enabled;
        maxCacheSize = builder.maxCacheSize;
        maxFileAgeForWriteMillis = builder.maxFileAgeForWriteMillis;
        minFileAgeForReadMillis = builder.minFileAgeForReadMillis;
        maxFileAgeForReadMillis = builder.maxFileAgeForReadMillis;
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

    public long getMaxFileAgeForWriteMillis() {
        return maxFileAgeForWriteMillis;
    }

    public long getMaxFileAgeForReadMillis() {
        return maxFileAgeForReadMillis;
    }

    public long getMinFileAgeForReadMillis() {
        return minFileAgeForReadMillis;
    }

    public static final class Builder {
        private boolean enabled = false;
        private int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;
        private long maxFileAgeForWriteMillis = TimeUnit.SECONDS.toMillis(30);
        private long minFileAgeForReadMillis = TimeUnit.SECONDS.toMillis(33);
        private long maxFileAgeForReadMillis = TimeUnit.HOURS.toMillis(18);

        /** Enables or disables disk buffering. */
        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /** Sets the max amount of time a file can receive new data. Default is 30 seconds. */
        public Builder setMaxFileAgeForWriteMillis(long maxFileAgeForWriteMillis) {
            this.maxFileAgeForWriteMillis = maxFileAgeForWriteMillis;
            return this;
        }

        /**
         * Sets the min amount of time that must pass before a file is read. This value must be
         * greater than maxFileAgeForWriteMillis.
         */
        public Builder setMinFileAgeForReadMillis(long minFileAgeForReadMillis) {
            this.minFileAgeForReadMillis = minFileAgeForReadMillis;
            return this;
        }

        /**
         * Sets the max age in ms for which a file is considered not-stale. Files older than this
         * will be dropped.
         */
        public Builder setMaxFileAgeForReadMillis(long maxFileAgeForReadMillis) {
            this.maxFileAgeForReadMillis = maxFileAgeForReadMillis;
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

        public DiskBufferingConfiguration build() {
            // See note in StorageConfiguration.getMinFileAgeForReadMillis()
            if (minFileAgeForReadMillis <= maxFileAgeForWriteMillis) {
                Logger logger = Logger.getLogger(DiskBufferingConfiguration.class.getName());
                logger.log(
                        Level.WARNING,
                        "minFileAgeForReadMillis must be greater than maxFileAgeForWriteMillis");
                logger.log(
                        Level.WARNING,
                        "overriding minFileAgeForReadMillis from "
                                + minFileAgeForReadMillis
                                + " to "
                                + minFileAgeForReadMillis
                                + 5);
                minFileAgeForReadMillis = maxFileAgeForWriteMillis + 5;
            }
            return new DiskBufferingConfiguration(this);
        }
    }
}
