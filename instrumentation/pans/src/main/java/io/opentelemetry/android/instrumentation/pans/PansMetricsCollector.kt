/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import android.util.Log
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.sdk.OpenTelemetrySdk
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Collects PANS (Per-Application Network Selection) metrics from Android system services.
 * This class periodically fetches network usage statistics and converts them to OpenTelemetry metrics.
 */
internal class PansMetricsCollector(
    private val context: Context,
    private val sdk: OpenTelemetrySdk,
    private val collectionIntervalMinutes: Long = DEFAULT_COLLECTION_INTERVAL_MINUTES,
) {
    private val meter: Meter = sdk.getMeter("io.opentelemetry.android.pans")
    private val isRunning = AtomicBoolean(false)
    private val netStatsManager: NetStatsManager = NetStatsManager(context)
    private val metricsExtractor: PANSMetricsExtractor = PANSMetricsExtractor(context, netStatsManager)

    /**
     * Starts periodic collection of PANS metrics.
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            Log.w(TAG, "PansMetricsCollector is already running")
            return
        }

        try {
            // Check if we have necessary permissions
            if (!netStatsManager.hasRequiredPermissions()) {
                Log.w(TAG, "Required permissions for PANS metrics collection not available")
                // Continue anyway - metrics may be available even with limited permissions
            }

            // Perform initial collection
            collectMetrics()

            // Schedule periodic collection
            schedulePeriodicCollection()

            Log.i(TAG, "PANS metrics collection started with interval: $collectionIntervalMinutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start PANS metrics collection", e)
            isRunning.set(false)
        }
    }

    /**
     * Stops the metric collection.
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            try {
                netStatsManager.close()
                Log.i(TAG, "PANS metrics collection stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error while stopping PANS metrics collection", e)
            }
        }
    }

    /**
     * Performs a single collection cycle of PANS metrics.
     */
    private fun collectMetrics() {
        try {
            val metrics = metricsExtractor.extractMetrics()
            recordMetrics(metrics)
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting PANS metrics", e)
        }
    }

    /**
     * Records extracted metrics using OpenTelemetry API.
     */
    private fun recordMetrics(metrics: PANSMetrics) {
        try {
            // Record per-app network usage counters
            val bytesTransmittedCounter =
                meter
                    .counterBuilder("network.pans.bytes_transmitted")
                    .setUnit("By")
                    .setDescription("Bytes transmitted via OEM networks")
                    .build()

            val bytesReceivedCounter =
                meter
                    .counterBuilder("network.pans.bytes_received")
                    .setUnit("By")
                    .setDescription("Bytes received via OEM networks")
                    .build()

            // Record app network preferences
            metrics.appNetworkUsage.forEach { usage ->
                bytesTransmittedCounter
                    .add(
                        usage.bytesTransmitted,
                        usage.attributes,
                    )
                bytesReceivedCounter
                    .add(
                        usage.bytesReceived,
                        usage.attributes,
                    )
            }

            // Record network preference changes
            metrics.preferenceChanges.forEach { change ->
                try {
                    val eventLogger = sdk.logsBridge["io.opentelemetry.android.pans"]
                    eventLogger
                        .logRecordBuilder()
                        .setEventName("network.pans.preference_changed")
                        .setAllAttributes(change.attributes)
                        .emit()
                } catch (e: Exception) {
                    Log.e(TAG, "Error recording preference change event", e)
                }
            }

            // Record OEM network availability
            meter
                .gaugeBuilder("network.pans.network_available")
                .setDescription("Whether OEM network is available")
                .ofLongs()
                .buildWithCallback { callback ->
                    metrics.networkAvailability.forEach { availability ->
                        callback.record(if (availability.isAvailable) 1L else 0L, availability.attributes)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording metrics", e)
        }
    }

    /**
     * Schedules periodic collection of metrics.
     */
    private fun schedulePeriodicCollection() {
        // This would integrate with the existing PeriodicWork service
        // For now, a simple implementation that would be replaced with actual scheduling
        Thread {
            while (isRunning.get()) {
                try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(collectionIntervalMinutes))
                    if (isRunning.get()) {
                        collectMetrics()
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic collection", e)
                }
            }
        }.start()
    }

    companion object {
        private const val TAG = "PansMetricsCollector"
        private const val DEFAULT_COLLECTION_INTERVAL_MINUTES = 15L
    }
}
