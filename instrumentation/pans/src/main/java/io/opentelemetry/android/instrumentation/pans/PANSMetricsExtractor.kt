/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import android.util.Log

/**
 * Extracts PANS (Per-Application Network Selection) metrics from system services
 * and converts them to OpenTelemetry-compatible data structures.
 */
internal class PANSMetricsExtractor(
    private val context: Context,
    private val netStatsManager: NetStatsManager,
) {
    private val connectivityManager = ConnectivityManagerWrapper(context)

    /**
     * Extracts all available PANS metrics.
     */
    fun extractMetrics(): PANSMetrics {
        try {
            val appNetworkUsage = extractAppNetworkUsage()
            val preferenceChanges = detectPreferenceChanges()
            val networkAvailability = extractNetworkAvailability()

            return PANSMetrics(
                appNetworkUsage = appNetworkUsage,
                preferenceChanges = preferenceChanges,
                networkAvailability = networkAvailability,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting PANS metrics", e)
            return PANSMetrics()
        }
    }

    /**
     * Extracts per-app network usage metrics.
     */
    private fun extractAppNetworkUsage(): List<AppNetworkUsage> {
        val usage = mutableListOf<AppNetworkUsage>()

        try {
            val stats = netStatsManager.getNetworkStats()

            stats.forEach { stat ->
                val attributes =
                    buildPansAttributes(
                        packageName = stat.packageName,
                        networkType = stat.networkType,
                        uid = stat.uid,
                    ) { builder ->
                        builder.put("timestamp_ms", stat.timestamp)
                    }

                usage.add(
                    AppNetworkUsage(
                        packageName = stat.packageName,
                        uid = stat.uid,
                        networkType = stat.networkType,
                        bytesTransmitted = stat.txBytes,
                        bytesReceived = stat.rxBytes,
                        attributes = attributes,
                    ),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting app network usage", e)
        }

        return usage
    }

    /**
     * Detects changes in network preferences for apps.
     * This is a simplified implementation that tracks preferences between collection cycles.
     */
    private fun detectPreferenceChanges(): List<PreferenceChange> {
        val changes = mutableListOf<PreferenceChange>()

        try {
            val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            // For each app, track preference changes
            val stats = netStatsManager.getNetworkStats()
            stats.forEach { stat ->
                val key = "${stat.uid}:${stat.packageName}"
                val currentPref = stat.networkType
                val previousPref = sharedPrefs.getString(key, null)

                if (previousPref != null && previousPref != currentPref) {
                    val attributes =
                        buildPreferenceChangeAttributes(
                            packageName = stat.packageName,
                            oldPreference = previousPref,
                            newPreference = currentPref,
                            uid = stat.uid,
                        )

                    changes.add(
                        PreferenceChange(
                            packageName = stat.packageName,
                            uid = stat.uid,
                            oldPreference = previousPref,
                            newPreference = currentPref,
                            timestamp = System.currentTimeMillis(),
                            attributes = attributes,
                        ),
                    )
                }
            }

            // Save current preferences for next cycle
            try {
                sharedPrefs
                    .edit()
                    .apply {
                        stats.forEach { stat ->
                            val key = "${stat.uid}:${stat.packageName}"
                            putString(key, stat.networkType)
                        }
                    }.apply()
            } catch (e: Exception) {
                Log.w(TAG, "Error saving preference cache", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting preference changes", e)
        }

        return changes
    }

    /**
     * Extracts network availability information.
     */
    private fun extractNetworkAvailability(): List<NetworkAvailability> {
        val availability = mutableListOf<NetworkAvailability>()

        try {
            val networks = connectivityManager.getAvailableNetworks()

            if (networks.any { it.isOemPaid }) {
                availability.add(
                    NetworkAvailability(
                        networkType = "OEM_PAID",
                        isAvailable = true,
                        attributes = buildNetworkAvailabilityAttributes("OEM_PAID"),
                    ),
                )
            }

            if (networks.any { it.isOemPrivate }) {
                availability.add(
                    NetworkAvailability(
                        networkType = "OEM_PRIVATE",
                        isAvailable = true,
                        attributes = buildNetworkAvailabilityAttributes("OEM_PRIVATE"),
                    ),
                )
            }

            // If no OEM networks detected, still report them as unavailable
            if (availability.isEmpty()) {
                availability.add(
                    NetworkAvailability(
                        networkType = "OEM_PAID",
                        isAvailable = false,
                        attributes = buildNetworkAvailabilityAttributes("OEM_PAID"),
                    ),
                )
                availability.add(
                    NetworkAvailability(
                        networkType = "OEM_PRIVATE",
                        isAvailable = false,
                        attributes = buildNetworkAvailabilityAttributes("OEM_PRIVATE"),
                    ),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting network availability", e)
        }

        return availability
    }

    companion object {
        private const val TAG = "PANSMetricsExtractor"
        private const val PREFS_NAME = "pans_preferences"
    }
}
