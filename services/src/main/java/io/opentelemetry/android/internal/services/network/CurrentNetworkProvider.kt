/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.internal.services.Service

// note: based on ideas from stack overflow:
// https://stackoverflow.com/questions/32547006/connectivitymanager-getnetworkinfoint-deprecated

/**
 * A provider of [CurrentNetwork] information. Registers itself in the Android ConnectivityManager
 * and listens for network changes.
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
interface CurrentNetworkProvider : Service {
    /** Returns up-to-date current network information.  */
    fun refreshNetworkStatus(): CurrentNetwork

    val currentNetwork: CurrentNetwork

    fun addNetworkChangeListener(listener: NetworkChangeListener)

    companion object {
        @JvmField
        val NO_NETWORK: CurrentNetwork = CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE)

        @JvmField
        val UNKNOWN_NETWORK: CurrentNetwork = CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN)
    }
}
