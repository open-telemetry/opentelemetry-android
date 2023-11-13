/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.config;

/** Configuration for disk buffering. */
public final class PersistenceConfiguration {
    public final boolean enabled;
    public final int maxCacheSize;

    private PersistenceConfiguration(Builder builder) {
        this.enabled = builder.enabled;
        this.maxCacheSize = builder.maxCacheSize;
    }

    public static Builder builder() {
        return new Builder(false, 60 * 1024 * 1024);
    }

    public static final class Builder {
        private boolean enabled;
        private int maxCacheSize;

        private Builder(boolean enabled, int maxCacheSize) {
            this.enabled = enabled;
            this.maxCacheSize = maxCacheSize;
        }

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

        public PersistenceConfiguration build() {
            return new PersistenceConfiguration(this);
        }
    }
}
