/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.internal.services.network.detector.NetworkDetector
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

// note: based on ideas from stack overflow:
// https://stackoverflow.com/questions/32547006/connectivitymanager-getnetworkinfoint-deprecated

/**
 * A provider of [CurrentNetwork] information. Registers itself in the Android [ ] and listens for network changes.
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
internal class CurrentNetworkProviderImpl(
    private val networkDetector: NetworkDetector,
    private val connectivityManager: ConnectivityManager,
    createNetworkMonitoringRequest: () -> NetworkRequest = ::createNetworkMonitoringRequest,
) : CurrentNetworkProvider {
    @Volatile
    override var currentNetwork: CurrentNetwork = CurrentNetworkProvider.UNKNOWN_NETWORK
        private set

    private val callbackRef = AtomicReference<NetworkCallback>()
    private val listeners: MutableList<NetworkChangeListener> = CopyOnWriteArrayList()

    init {
        startMonitoring(createNetworkMonitoringRequest)
    }

    private fun startMonitoring(createNetworkMonitoringRequest: () -> NetworkRequest) {
        refreshNetworkStatus()
        try {
            registerNetworkCallbacks(createNetworkMonitoringRequest)
        } catch (e: Exception) {
            // if this fails, we'll go without network change events.
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "Failed to register network callbacks. Automatic network monitoring is disabled.",
                e,
            )
        }
    }

    private fun registerNetworkCallbacks(createNetworkMonitoringRequest: () -> NetworkRequest) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerNetworkCallbackApi24()
        } else {
            val networkRequest = createNetworkMonitoringRequest()
            val callback: NetworkCallback = ConnectionMonitor()
            connectivityManager.registerNetworkCallback(networkRequest, callback)
            callbackRef.set(callback)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun registerNetworkCallbackApi24() {
        val callback: NetworkCallback = ConnectionMonitor()
        connectivityManager.registerDefaultNetworkCallback(callback)
        callbackRef.set(callback)
    }

    /** Returns up-to-date [current network information][CurrentNetwork].  */
    override fun refreshNetworkStatus(): CurrentNetwork {
        currentNetwork =
            try {
                networkDetector.detectCurrentNetwork()
            } catch (e: Exception) {
                // guard against security issues/bugs when accessing the Android connectivityManager.
                // see: https://issuetracker.google.com/issues/175055271
                CurrentNetworkProvider.UNKNOWN_NETWORK
            }
        return currentNetwork
    }

    override fun addNetworkChangeListener(listener: NetworkChangeListener) {
        listeners.add(listener)
    }

    private fun notifyListeners(activeNetwork: CurrentNetwork) {
        for (listener in listeners) {
            listener.onNetworkChange(activeNetwork)
        }
    }

    override fun close() {
        val callback = callbackRef.get()
        if (callback != null) {
            connectivityManager.unregisterNetworkCallback(callback)
            listeners.clear()
            callbackRef.set(null)
        }
    }

    private inner class ConnectionMonitor : NetworkCallback() {
        override fun onAvailable(network: Network) {
            val activeNetwork = refreshNetworkStatus()
            Log.d(RumConstants.OTEL_RUM_LOG_TAG, "  onAvailable: currentNetwork=$activeNetwork")

            notifyListeners(activeNetwork)
        }

        override fun onLost(network: Network) {
            // it seems that the "currentNetwork" is still the one that is being lost, so for
            // this method, we'll force it to be NO_NETWORK, rather than relying on the
            // ConnectivityManager to have the right
            // state at the right time during this event.
            val currentNetwork = CurrentNetworkProvider.NO_NETWORK
            this@CurrentNetworkProviderImpl.currentNetwork = currentNetwork
            Log.d(RumConstants.OTEL_RUM_LOG_TAG, "  onLost: currentNetwork=$currentNetwork")

            notifyListeners(currentNetwork)
        }
    }

    companion object {
        private fun createNetworkMonitoringRequest(): NetworkRequest {
            // note: this throws an NPE when running in junit without robolectric, due to Android
            return NetworkRequest
                .Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build()
        }
    }
}
