/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.annotation.VisibleForTesting
import io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG

/**
 * Auto-detects optimal telemetry export frequency based on device conditions.
 *
 * This class analyzes device state and suggests an appropriate export interval:
 * - Devices under memory pressure: Increase interval (export less frequently)
 * - Devices on battery (not charging): Increase interval (preserve battery)
 * - Devices with low battery: Increase interval significantly
 * - Normal conditions: Use default interval
 *
 * The auto-detected value can be overridden by explicitly setting
 * [DiskBufferingConfig.exportScheduleDelayMillis].
 */
internal object ExportScheduleAutoDetector {
    private const val DEFAULT_EXPORT_INTERVAL_MILLIS = 10000L // 10 seconds
    private const val BATTERY_SAVER_INTERVAL_MILLIS = 30000L // 30 seconds
    private const val LOW_MEMORY_INTERVAL_MILLIS = 20000L // 20 seconds

    /**
     * Detects optimal export schedule delay based on current device state.
     *
     * @param context Android context for accessing system services
     * @param userConfiguredDelay User-explicitly configured delay, or null for auto-detection.
     *                            If provided, this value is always used (overrides auto-detection).
     * @return Optimal export interval in milliseconds
     */
    fun detectOptimalExportDelay(
        context: Context,
        userConfiguredDelay: Long?,
    ): Long {
        // If user explicitly configured a delay, always respect it
        if (userConfiguredDelay != null && userConfiguredDelay != DEFAULT_EXPORT_INTERVAL_MILLIS) {
            Log.d(OTEL_RUM_LOG_TAG, "Using user-configured export delay: $userConfiguredDelay ms")
            return userConfiguredDelay
        }

        val detectedDelay = detectBasedOnDeviceConditions(context)

        if (detectedDelay != DEFAULT_EXPORT_INTERVAL_MILLIS) {
            Log.i(
                OTEL_RUM_LOG_TAG,
                "Auto-detected export delay: $detectedDelay ms (default: $DEFAULT_EXPORT_INTERVAL_MILLIS ms)",
            )
        }

        return detectedDelay
    }

    /**
     * Analyzes device conditions and returns recommended export interval.
     */
    private fun detectBasedOnDeviceConditions(context: Context): Long {
        val reasons = mutableListOf<String>()
        var recommendedDelay = DEFAULT_EXPORT_INTERVAL_MILLIS

        // Check battery status
        val batteryDelay = checkBatteryStatus(context)
        if (batteryDelay > DEFAULT_EXPORT_INTERVAL_MILLIS) {
            reasons.add("Battery status: ${batteryDelay}ms")
            recommendedDelay = maxOf(recommendedDelay, batteryDelay)
        }

        // Check memory pressure
        val memoryDelay = checkMemoryPressure(context)
        if (memoryDelay > DEFAULT_EXPORT_INTERVAL_MILLIS) {
            reasons.add("Memory pressure: ${memoryDelay}ms")
            recommendedDelay = maxOf(recommendedDelay, memoryDelay)
        }

        if (reasons.isNotEmpty()) {
            Log.d(OTEL_RUM_LOG_TAG, "Export delay auto-detection reasons: ${reasons.joinToString(", ")}")
        }

        return recommendedDelay
    }

    /**
     * Checks battery status and suggests interval adjustment.
     *
     * Returns:
     * - [BATTERY_SAVER_INTERVAL_MILLIS] if device is on battery saver or low battery
     * - [DEFAULT_EXPORT_INTERVAL_MILLIS] if device is charging or has healthy battery
     */
    @VisibleForTesting
    internal fun checkBatteryStatus(context: Context): Long {
        return try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (batteryIntent != null) {
                val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

                val isCharging = plugged != 0
                val isBatteryLow = status == BatteryManager.BATTERY_STATUS_UNKNOWN || level < 0

                // If battery is below 20% and not charging, use extended interval
                if (level in 1..19 && !isCharging) {
                    return BATTERY_SAVER_INTERVAL_MILLIS
                }

                // If we can't determine battery status reliably, be conservative
                if (isBatteryLow) {
                    return BATTERY_SAVER_INTERVAL_MILLIS
                }
            }

            DEFAULT_EXPORT_INTERVAL_MILLIS
        } catch (e: Exception) {
            Log.d(OTEL_RUM_LOG_TAG, "Failed to check battery status: ${e.message}")
            DEFAULT_EXPORT_INTERVAL_MILLIS
        }
    }

    /**
     * Checks memory pressure and suggests interval adjustment.
     *
     * Returns:
     * - [LOW_MEMORY_INTERVAL_MILLIS] if device is under memory pressure
     * - [DEFAULT_EXPORT_INTERVAL_MILLIS] if device has healthy memory
     */
    @VisibleForTesting
    internal fun checkMemoryPressure(context: Context): Long {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (activityManager != null) {
                val runtime = Runtime.getRuntime()
                val maxMemory = runtime.maxMemory()
                val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                val memoryUsagePercent = usedMemory.toFloat() / maxMemory

                // If using more than 85% of available memory, extend interval
                if (memoryUsagePercent > 0.85f) {
                    Log.d(
                        OTEL_RUM_LOG_TAG,
                        "Memory pressure detected: ${(memoryUsagePercent * 100).toInt()}% used",
                    )
                    return LOW_MEMORY_INTERVAL_MILLIS
                }
            }

            DEFAULT_EXPORT_INTERVAL_MILLIS
        } catch (e: Exception) {
            Log.d(OTEL_RUM_LOG_TAG, "Failed to check memory pressure: ${e.message}")
            DEFAULT_EXPORT_INTERVAL_MILLIS
        }
    }
}
