/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector

import android.net.ConnectivityManager
import android.net.ConnectivityManager.TYPE_ETHERNET
import android.net.ConnectivityManager.TYPE_MOBILE
import android.net.ConnectivityManager.TYPE_VPN
import android.net.ConnectivityManager.TYPE_WIFI
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState.TRANSPORT_CELLULAR
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState.TRANSPORT_VPN
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState.TRANSPORT_WIFI
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState.TRANSPORT_WIRED
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider.UNKNOWN_NETWORK

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
internal class SimpleNetworkDetector(
    private val connectivityManager: ConnectivityManager,
) : NetworkDetector {
    override fun detectCurrentNetwork(): CurrentNetwork =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            detectUsingModernApi()
        } else {
            detectUsingLegacyApi()
        }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private fun detectUsingModernApi(): CurrentNetwork {
        val network = connectivityManager.activeNetwork ?: return CurrentNetworkProvider.NO_NETWORK

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return UNKNOWN_NETWORK

        fun hasTransport(capability: Int): Boolean = capabilities.hasCapability(capability)
        return when {
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> network(TRANSPORT_CELLULAR)
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> network(TRANSPORT_WIFI)
            hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> network(TRANSPORT_VPN)
            hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> network(TRANSPORT_WIRED)
            else -> UNKNOWN_NETWORK
        }
    }

    @Suppress("deprecation")
    private fun detectUsingLegacyApi(): CurrentNetwork {
        val activeNetwork =
            connectivityManager.activeNetworkInfo
                ?: return CurrentNetworkProvider.NO_NETWORK

        val network = buildFromSubType(activeNetwork.subtypeName)
        return when (activeNetwork.type) {
            TYPE_MOBILE -> network(TRANSPORT_CELLULAR)
            TYPE_WIFI -> network(TRANSPORT_WIFI)
            TYPE_VPN -> network(TRANSPORT_VPN)
            TYPE_ETHERNET -> network(TRANSPORT_WIRED)
            else -> UNKNOWN_NETWORK
        }
    }

    private fun buildFromSubType(subType: String?): (NetworkState) -> CurrentNetwork =
        { CurrentNetwork.builder(it).subType(subType).build() }

    private fun network(state: NetworkState): CurrentNetwork = CurrentNetwork.builder(state).build()
}
