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
const val DEFAULT_EXPORT_SCHEDULE_DELAY_MILLIS: Long = 60000L // 1 minute

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
         * The delay in milliseconds between consecutive export attempts. Defaults to 1 minute (60000 ms).
         *
         * This value controls how frequently the SDK attempts to export buffered signals from disk.
         * The configured value represents the minimum delay between export attempts. Due to the
         * periodic work scheduling mechanism, the actual export frequency may be limited by the
         * loop interval of the periodic work executor (default: 60 seconds).
         *
         * Recommended values:
         * - 60000 ms (1 minute) or higher: Standard configuration, provides good balance between
         *   data freshness and resource consumption. This matches the default periodic work loop
         *   interval and ensures exports happen at the configured frequency.
         * - 300000 ms (5 minutes) or higher: For high-volume scenarios where reducing backend load
         *   and battery consumption is critical. Suitable for applications where near-real-time
         *   data is not essential.
         * - Values less than 60000 ms: Not recommended. While the SDK supports values down to
         *   1000 ms (1 second), the periodic work executor's 60-second loop interval may prevent
         *   the configured frequency from being achieved. If you configure a value less than
         *   60 seconds, the actual export frequency will still be approximately 60 seconds.
         *
         * Important: For each 8-hour workday, configure thoughtfully to balance between:
         * - Data freshness (prefer lower values)
         * - Device battery consumption (prefer higher values)
         * - Backend load (prefer higher values)
         *
         * Example impact: A 10-second export interval means ~2880 export attempts per 8-hour day.
         * A 60-second interval reduces this to ~480 attempts. A 5-minute interval reduces it to ~96 attempts.
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
                } else if (exportScheduleDelayMillis < 60000L) {
                    // Warn users about the periodic work loop interval limitation
                    Log.w(
                        OTEL_RUM_LOG_TAG,
                        "exportScheduleDelayMillis is set to $exportScheduleDelayMillis ms, which is less " +
                            "than the periodic work loop interval (60000 ms)",
                    )
                    Log.w(
                        OTEL_RUM_LOG_TAG,
                        "The actual export frequency may be limited to approximately 60 seconds " +
                            "regardless of the configured value",
                    )
                    Log.w(
                        OTEL_RUM_LOG_TAG,
                        "Consider using 60000 ms (1 minute) or higher for more predictable export behavior",
                    )
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
