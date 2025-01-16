/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider

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
                ?: return CurrentNetworkProvider.UNKNOWN_NETWORK

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return buildCurrentNetwork(NetworkState.TRANSPORT_CELLULAR, "")
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return buildCurrentNetwork(NetworkState.TRANSPORT_WIFI, "")
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            return buildCurrentNetwork(NetworkState.TRANSPORT_VPN, "")
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return buildCurrentNetwork(NetworkState.TRANSPORT_WIRED, "")
        }

        return CurrentNetworkProvider.UNKNOWN_NETWORK
    }

    @Suppress("deprecation")
    private fun detectUsingLegacyApi(): CurrentNetwork {
        val activeNetwork =
            connectivityManager.activeNetworkInfo
                ?: return CurrentNetworkProvider.NO_NETWORK

        return when (activeNetwork.type) {
            ConnectivityManager.TYPE_MOBILE ->
                buildCurrentNetwork(
                    NetworkState.TRANSPORT_CELLULAR,
                    activeNetwork.subtypeName,
                )

            ConnectivityManager.TYPE_WIFI ->
                buildCurrentNetwork(
                    NetworkState.TRANSPORT_WIFI,
                    activeNetwork.subtypeName,
                )

            ConnectivityManager.TYPE_VPN ->
                buildCurrentNetwork(
                    NetworkState.TRANSPORT_VPN,
                    activeNetwork.subtypeName,
                )

            ConnectivityManager.TYPE_ETHERNET ->
                buildCurrentNetwork(
                    NetworkState.TRANSPORT_WIRED,
                    activeNetwork.subtypeName,
                )

            else -> CurrentNetworkProvider.UNKNOWN_NETWORK
        }
    }

    private fun buildCurrentNetwork(
        state: NetworkState,
        subType: String,
    ): CurrentNetwork = CurrentNetwork.builder(state).subType(subType).build()
}
