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
import androidx.annotation.RequiresApi
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState.TRANSPORT_CELLULAR
import io.opentelemetry.android.internal.services.network.CarrierFinder
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.SubTypeFinder

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
interface NetworkDetector {
    fun detectCurrentNetwork(): CurrentNetwork

    companion object {
        @JvmStatic
        fun create(context: Context): NetworkDetector {
            // TODO: Use ServiceManager to get the ConnectivityManager, TelephonyManager or similar (not yet managed/abstracted)
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val carrierFinder = CarrierFinder(context, telephonyManager)
            val subTypeFinder = SubTypeFinder(context, telephonyManager)

            return object : NetworkDetector {
                override fun detectCurrentNetwork(): CurrentNetwork =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        detectNetworkPostApi23(connectivityManager)
                    } else {
                        detectNetworkPreApi23(connectivityManager)
                    }

                @RequiresApi(Build.VERSION_CODES.M)
                private fun detectNetworkPostApi23(connectivityManager: ConnectivityManager): CurrentNetwork {
                    val network = connectivityManager.activeNetwork ?: return CurrentNetworkProvider.NO_NETWORK
                    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return CurrentNetworkProvider.UNKNOWN_NETWORK

                    fun hasTransport(transport: Int): Boolean = capabilities.hasTransport(transport)
                    return when {
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> buildCellularNetwork()
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                            buildNetwork(
                                NetworkState.TRANSPORT_WIFI,
                            )
                        hasTransport(NetworkCapabilities.TRANSPORT_VPN) ->
                            buildNetwork(
                                NetworkState.TRANSPORT_VPN,
                            )
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                            buildNetwork(
                                NetworkState.TRANSPORT_WIRED,
                            )
                        else -> CurrentNetworkProvider.UNKNOWN_NETWORK
                    }
                }

                @Suppress("deprecation")
                private fun detectNetworkPreApi23(connectivityManager: ConnectivityManager): CurrentNetwork {
                    val activeNetwork = connectivityManager.activeNetworkInfo ?: return CurrentNetworkProvider.NO_NETWORK
                    return when (activeNetwork.type) {
                        ConnectivityManager.TYPE_MOBILE -> buildCellularNetwork()
                        ConnectivityManager.TYPE_WIFI -> buildNetwork(NetworkState.TRANSPORT_WIFI)
                        ConnectivityManager.TYPE_VPN -> buildNetwork(NetworkState.TRANSPORT_VPN)
                        ConnectivityManager.TYPE_ETHERNET -> buildNetwork(NetworkState.TRANSPORT_WIRED)
                        else -> CurrentNetworkProvider.UNKNOWN_NETWORK
                    }
                }

                /**
                 * Builds a cellular network with carrier and subtype information.
                 */
                fun buildCellularNetwork(): CurrentNetwork {
                    val carrier = carrierFinder.get()
                    val subType = subTypeFinder.get()
                    return CurrentNetwork
                        .builder(TRANSPORT_CELLULAR)
                        .carrier(carrier)
                        .subType(subType)
                        .build()
                }

                /**
                 * Builds a network for non-cellular networks.
                 */
                fun buildNetwork(networkState: NetworkState): CurrentNetwork = CurrentNetwork.builder(networkState).build()
            }
        }
    }
}
