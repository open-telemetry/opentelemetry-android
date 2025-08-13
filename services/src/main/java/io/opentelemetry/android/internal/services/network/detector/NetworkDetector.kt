/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState.TRANSPORT_CELLULAR
import io.opentelemetry.android.internal.services.network.CarrierFinder
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.NetworkUtils

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
interface NetworkDetector {
    fun detectCurrentNetwork(): CurrentNetwork

    companion object {
        @JvmStatic
        fun create(context: Context): NetworkDetector {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val carrierFinder = CarrierFinder(context, telephonyManager)

            return object : NetworkDetector {
                override fun detectCurrentNetwork(): CurrentNetwork =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        detectNetworkPostApi23(connectivityManager)
                    } else {
                        detectNetworkPreApi23(connectivityManager)
                    }

                @RequiresApi(Build.VERSION_CODES.M)
                private fun detectNetworkPostApi23(connectivityManager: ConnectivityManager): CurrentNetwork {
                    val network = connectivityManager.activeNetwork
                    val capabilities =
                        network?.let {
                            connectivityManager.getNetworkCapabilities(it)
                        }

                    return when {
                        network == null -> CurrentNetworkProvider.NO_NETWORK
                        capabilities == null -> CurrentNetworkProvider.UNKNOWN_NETWORK
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                            buildCellularNetwork()
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                            buildNetwork(NetworkState.TRANSPORT_WIFI)
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ->
                            buildNetwork(NetworkState.TRANSPORT_VPN)
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                            buildNetwork(NetworkState.TRANSPORT_WIRED)
                        else -> CurrentNetworkProvider.UNKNOWN_NETWORK
                    }
                }

                @Suppress("deprecation")
                private fun detectNetworkPreApi23(connectivityManager: ConnectivityManager): CurrentNetwork {
                    val activeNetwork =
                        connectivityManager.activeNetworkInfo
                            ?: return CurrentNetworkProvider.NO_NETWORK
                    return when (activeNetwork.type) {
                        ConnectivityManager.TYPE_MOBILE -> buildCellularNetwork()
                        ConnectivityManager.TYPE_WIFI ->
                            buildNetwork(NetworkState.TRANSPORT_WIFI)
                        ConnectivityManager.TYPE_VPN ->
                            buildNetwork(NetworkState.TRANSPORT_VPN)
                        ConnectivityManager.TYPE_ETHERNET ->
                            buildNetwork(NetworkState.TRANSPORT_WIRED)
                        else -> CurrentNetworkProvider.UNKNOWN_NETWORK
                    }
                }

                /**
                 * Builds a network for non-cellular networks.
                 */
                fun buildNetwork(networkState: NetworkState) = CurrentNetwork.builder(networkState).build()

                /**
                 * Builds a cellular network with carrier and subtype information.
                 */
                fun buildCellularNetwork(): CurrentNetwork {
                    val carrier = carrierFinder.get()
                    val subType = findSubtype(context, telephonyManager)
                    return CurrentNetwork
                        .builder(TRANSPORT_CELLULAR)
                        .carrier(carrier)
                        .subType(subType)
                        .build()
                }

                /**
                 * Gets the current cellular network subtype with proper permission and API level handling.
                 *
                 * @param context The application context for permission checking
                 * @param telephonyManager The TelephonyManager instance
                 * @return Network subtype name or null if unavailable or permission denied
                 */
                @Suppress("MissingPermission")
                private fun findSubtype(context: Context, telephonyManager: TelephonyManager): String? {
                    if (!NetworkUtils.hasReadPhoneStatePermission(context)) {
                        return null
                    }

                    return try {
                        val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            getNetworkSubTypePostApi24(telephonyManager)
                        } else {
                            @Suppress("DEPRECATION")
                            telephonyManager.networkType
                        }
                        NetworkUtils.getNetworkTypeName(networkType)
                    } catch (e: SecurityException) {
                        Log.w(
                            RumConstants.OTEL_RUM_LOG_TAG,
                            "SecurityException when accessing network type",
                            e
                        )
                        null
                    }
                }

                @RequiresApi(Build.VERSION_CODES.N)
                @Suppress("MissingPermission")
                private fun getNetworkSubTypePostApi24(telephonyManager: TelephonyManager): Int {
                    return telephonyManager.dataNetworkType
                }
            }
        }
    }
}
