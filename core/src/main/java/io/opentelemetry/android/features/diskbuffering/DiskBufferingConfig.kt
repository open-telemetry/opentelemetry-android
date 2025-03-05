/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering

import android.util.Log
import io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG
import java.io.File
import java.util.concurrent.TimeUnit

const val DEFAULT_MAX_CACHE_SIZE: Int = 10 * 1024 * 1024
const val MAX_CACHE_FILE_SIZE: Int = 1024 * 1024
const val DEFAULT_MAX_FILE_AGE_FOR_WRITE_MS = 30L
const val DEFAULT_MIN_FILE_AGE_FOR_READ_MS = 33L
const val DEFAULT_MAX_FILE_AGE_FOR_READ_MS = 18L

data class DiskBufferingConfig
    @JvmOverloads
    constructor(
        val enabled: Boolean = false,
        val maxCacheSize: Int = DEFAULT_MAX_CACHE_SIZE,
        val maxFileAgeForWriteMillis: Long = TimeUnit.SECONDS.toMillis(DEFAULT_MAX_FILE_AGE_FOR_WRITE_MS),
        val minFileAgeForReadMillis: Long = TimeUnit.SECONDS.toMillis(DEFAULT_MIN_FILE_AGE_FOR_READ_MS),
        val maxFileAgeForReadMillis: Long = TimeUnit.HOURS.toMillis(DEFAULT_MAX_FILE_AGE_FOR_READ_MS),
        val maxCacheFileSize: Int = MAX_CACHE_FILE_SIZE,
        val debugEnabled: Boolean = false,
        /**
         * The directory where the SDK stores the buffered signals before they are exported. If
         * `null`, a default directory inside the application's cache directory will be used.
         */
        val signalsBufferDir: File? = null,
    ) {
        companion object {
            /**
             * Convenience factory method that validates the min/max and fixes
             * those up if needed. Users should prefer this method over the
             * vanilla non-validating constructor.
             */
            @JvmOverloads
            @JvmStatic
            fun create(
                enabled: Boolean = false,
                maxCacheSize: Int = DEFAULT_MAX_CACHE_SIZE,
                maxFileAgeForWriteMillis: Long = TimeUnit.SECONDS.toMillis(30),
                minFileAgeForReadMillis: Long = TimeUnit.SECONDS.toMillis(33),
                maxFileAgeForReadMillis: Long = TimeUnit.HOURS.toMillis(18),
                maxCacheFileSize: Int = MAX_CACHE_FILE_SIZE,
                debugEnabled: Boolean = false,
                signalsBufferDir: File? = null,
            ): DiskBufferingConfig {
                var minRead = minFileAgeForReadMillis
                if (minFileAgeForReadMillis <= maxFileAgeForWriteMillis) {
                    minRead = maxFileAgeForWriteMillis + 5
                    Log.w(OTEL_RUM_LOG_TAG, "minFileAgeForReadMillis must be greater than maxFileAgeForWriteMillis")
                    Log.w(OTEL_RUM_LOG_TAG, "overriding minFileAgeForReadMillis from $minFileAgeForReadMillis to $minRead")
                }
                return DiskBufferingConfig(
                    enabled = enabled,
                    maxCacheSize = maxCacheSize,
                    maxFileAgeForWriteMillis = maxFileAgeForWriteMillis,
                    minFileAgeForReadMillis = minRead,
                    maxFileAgeForReadMillis = maxFileAgeForReadMillis,
                    maxCacheFileSize = maxCacheFileSize,
                    debugEnabled = debugEnabled,
                    signalsBufferDir = signalsBufferDir,
                )
            }
        }
    }
