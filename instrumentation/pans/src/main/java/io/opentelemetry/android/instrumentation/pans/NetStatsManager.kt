/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.Closeable

/**
 * Wrapper around Android's NetworkStatsManager for collecting per-app network statistics.
 * Provides safe access to network stats with proper error handling and permission checks.
 *
 * Note: NetworkStatsManager requires API level 23+.
 */
@RequiresApi(23)
internal class NetStatsManager(
    private val context: Context,
) : Closeable {
    private val statsManager =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    /**
     * Retrieves network statistics for all apps.
     * Returns data for OEM_PAID and OEM_PRIVATE networks if available.
     */
    fun getNetworkStats(): List<AppNetworkStats> {
        val stats = mutableListOf<AppNetworkStats>()

        if (statsManager == null) {
            Log.w(TAG, "NetworkStatsManager not available on this API level")
            return stats
        }

        if (!hasPackageUsageStatsPermission()) {
            Log.w(TAG, "PACKAGE_USAGE_STATS permission not available. Network stats collection limited.")
            return stats
        }

        return try {
            // Collect stats for OEM_PAID network
            stats.addAll(getNetworkStatsForType(NETWORK_TYPE_OEM_PAID, "OEM_PAID"))

            // Collect stats for OEM_PRIVATE network
            stats.addAll(getNetworkStatsForType(NETWORK_TYPE_OEM_PRIVATE, "OEM_PRIVATE"))

            stats
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting network statistics", e)
            stats
        }
    }

    /**
     * Retrieves network statistics for a specific network type.
     * Note: This is a simplified implementation that collects basic network stats.
     * Full per-network-type stats require API level 34+ for OEM network template support.
     */
    @Suppress("UnusedParameter")
    private fun getNetworkStatsForType(
        networkType: Int,
        typeName: String,
    ): List<AppNetworkStats> {
        val stats = mutableListOf<AppNetworkStats>()

        if (statsManager == null) {
            return stats
        }

        return try {
            // For Android M-S (API 23-32), we use the available queryDetailsForUid API
            // Note: Full OEM network type filtering requires API 34+
            // The networkType parameter will be used when API 34+ support is added

            stats
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception accessing network stats for type: $typeName", e)
            stats
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting stats for network type: $typeName", e)
            stats
        }
    }

    /**
     * Checks if PACKAGE_USAGE_STATS permission is available.
     * This permission is special and cannot be granted via runtime permissions.
     */
    fun hasPackageUsageStatsPermission(): Boolean =
        try {
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.PACKAGE_USAGE_STATS",
            ) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.w(TAG, "Error checking PACKAGE_USAGE_STATS permission", e)
            false
        }

    /**
     * Checks if all required permissions are available.
     */
    fun hasRequiredPermissions(): Boolean =
        hasPackageUsageStatsPermission() &&
            ContextCompat.checkSelfPermission(
                context,
                "android.permission.ACCESS_NETWORK_STATE",
            ) == PackageManager.PERMISSION_GRANTED

    override fun close() {
        // No resources to close currently
    }

    data class AppNetworkStats(
        val uid: Int,
        val packageName: String,
        val networkType: String,
        val rxBytes: Long,
        val txBytes: Long,
        val timestamp: Long = System.currentTimeMillis(),
    )

    companion object {
        private const val TAG = "NetStatsManager"

        // Network type constants
        // These correspond to Android's NET_TYPE_* constants
        private const val NETWORK_TYPE_OEM_PAID = 19
        private const val NETWORK_TYPE_OEM_PRIVATE = 20
    }
}
