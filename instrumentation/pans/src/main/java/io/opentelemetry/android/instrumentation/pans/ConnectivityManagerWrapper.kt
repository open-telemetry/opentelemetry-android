/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * Wrapper around Android's ConnectivityManager for monitoring network state and preferences.
 * This class provides utilities to detect available networks and their capabilities.
 * Note: Most methods require API level 23+ for proper functionality.
 */
@RequiresApi(23)
internal class ConnectivityManagerWrapper(
    private val context: Context,
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * Checks if a specific network capability is available.
     * OEM_PAID and OEM_PRIVATE are network capabilities that indicate OEM-managed networks.
     */
    fun hasNetworkCapability(capabilityType: Int): Boolean {
        return try {
            val network = connectivityManager?.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(capabilityType)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking network capability: $capabilityType", e)
            false
        }
    }

    /**
     * Gets all available networks with their capabilities.
     */
    @android.annotation.SuppressLint("WrongConstant")
    fun getAvailableNetworks(): List<NetworkInfo> {
        val networks = mutableListOf<NetworkInfo>()
        return try {
            val allNetworks = connectivityManager?.allNetworks ?: return networks
            allNetworks.forEach { network ->
                try {
                    val capabilities = connectivityManager?.getNetworkCapabilities(network)
                    if (capabilities != null) {
                        networks.add(
                            NetworkInfo(
                                isOemPaid = capabilities.hasCapability(CAP_OEM_PAID),
                                isOemPrivate = capabilities.hasCapability(CAP_OEM_PRIVATE),
                                isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                                isConnected = isNetworkConnected(network),
                            ),
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error getting network capabilities", e)
                }
            }
            networks
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available networks", e)
            networks
        }
    }

    /**
     * Checks if a specific network is currently connected.
     */
    fun isNetworkConnected(network: android.net.Network): Boolean =
        try {
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Error checking network connection", e)
            false
        }

    /**
     * Gets the active network or null if none is active.
     */
    fun getActiveNetwork(): android.net.Network? =
        try {
            connectivityManager?.activeNetwork
        } catch (e: Exception) {
            Log.w(TAG, "Error getting active network", e)
            null
        }

    /**
     * Checks if ACCESS_NETWORK_STATE permission is granted.
     */
    fun hasAccessNetworkStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            "android.permission.ACCESS_NETWORK_STATE",
        ) == PackageManager.PERMISSION_GRANTED

    data class NetworkInfo(
        val isOemPaid: Boolean = false,
        val isOemPrivate: Boolean = false,
        val isMetered: Boolean = false,
        val isConnected: Boolean = false,
    )

    companion object {
        private const val TAG = "ConnMgrWrapper"

        // Network capability constants for OEM networks
        // These are defined as constants to support various Android versions
        private const val CAP_OEM_PAID = 19 // NET_CAPABILITY_OEM_PAID
        private const val CAP_OEM_PRIVATE = 20 // NET_CAPABILITY_OEM_PRIVATE
    }
}
