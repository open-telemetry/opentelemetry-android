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
const val DEFAULT_EXPORT_SCHEDULE_DELAY_MILLIS: Long = 10000L // 10 seconds

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
        /**
         * The delay in milliseconds between consecutive export attempts. Defaults to 10 seconds (10000 ms).
         *
         * This value controls how frequently the SDK attempts to export buffered signals from disk.
         * The configured value represents the minimum delay between export attempts.
         *
         * Trade-offs to consider:
         * - Lower values (e.g., 10 seconds): More frequent exports mean fresher data in RUM sessions,
         *   but higher resource consumption (CPU, disk I/O, network activity) and potentially higher
         *   backend load from more frequent requests.
         * - Higher values (e.g., 60 seconds or more): Reduced resource consumption and backend load,
         *   but longer delay before data becomes available in RUM sessions.
         *
         * Configuration recommendations:
         * - 10000 ms (10 seconds): Default value, provides balance between data freshness and
         *   resource consumption for typical applications.
         * - 5000 ms (5 seconds) or lower: For applications with critical real-time monitoring needs,
         *   where fresher data is worth the additional resource cost.
         * - 30000 ms (30 seconds) or higher: For applications with high telemetry volume where reducing
         *   backend load and device resource consumption is prioritized over data freshness.
         *
         * Performance considerations:
         * - Each export cycle may involve reading, serializing, and transmitting buffered signals.
         *   Higher export frequency means more frequent resource usage.
         * - Lower export frequency means more data accumulates per export cycle, which could impact
         *   memory usage and delay in data availability.
         *
         * Minimum supported value: 1000 ms (1 second). Values less than this will be automatically
         * increased to 1000 ms with a warning.
         *
         * Best practice: Test your configuration with your specific telemetry volume and workload
         * to find the optimal balance for your use case.
         */
        val exportScheduleDelayMillis: Long = DEFAULT_EXPORT_SCHEDULE_DELAY_MILLIS,
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
                exportScheduleDelayMillis: Long = DEFAULT_EXPORT_SCHEDULE_DELAY_MILLIS,
            ): DiskBufferingConfig {
                var minRead = minFileAgeForReadMillis
                if (minFileAgeForReadMillis <= maxFileAgeForWriteMillis) {
                    minRead = maxFileAgeForWriteMillis + 5
                    Log.w(OTEL_RUM_LOG_TAG, "minFileAgeForReadMillis must be greater than maxFileAgeForWriteMillis")
                    Log.w(OTEL_RUM_LOG_TAG, "overriding minFileAgeForReadMillis from $minFileAgeForReadMillis to $minRead")
                }
                var validatedExportDelay = exportScheduleDelayMillis
                if (exportScheduleDelayMillis < 1000L) {
                    validatedExportDelay = 1000L
                    Log.w(OTEL_RUM_LOG_TAG, "exportScheduleDelayMillis must be at least 1000 ms (1 second)")
                    Log.w(OTEL_RUM_LOG_TAG, "overriding exportScheduleDelayMillis from $exportScheduleDelayMillis to $validatedExportDelay")
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
                    exportScheduleDelayMillis = validatedExportDelay,
                )
            }
        }
    }
