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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

// note: based on ideas from stack overflow:
// https://stackoverflow.com/questions/32547006/connectivitymanager-getnetworkinfoint-deprecated

/**
 * A provider of [CurrentNetwork] information. Registers itself in the Android [ConnectivityManager] and listens for network changes.
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
internal class CurrentNetworkProviderImpl(
    private val networkDetector: NetworkDetector,
    private val connectivityManager: ConnectivityManager,
    private val phoneStatePermissionGrantCheckTrigger: PermissionGrantCheckTrigger = PermissionGrantCheckTrigger.NONE,
    createNetworkMonitoringRequest: () -> NetworkRequest = ::createNetworkMonitoringRequest,
) : CurrentNetworkProvider {
    // Observations run on the callback thread and, after the permission grant, on the caller's
    // thread. Each takes a sequence number before detecting, so a slower, older one cannot
    // overwrite a newer one.
    private val observationSequence = AtomicLong()
    private val latestObservation = AtomicReference(Observation(0, CurrentNetworkProvider.UNKNOWN_NETWORK))

    override val currentNetwork: CurrentNetwork
        get() {
            // The subtype and carrier details need the phone state permission, so the network is
            // re-detected once it is granted.
            if (phoneStatePermissionGrantCheckTrigger.consumeTrigger()) {
                // A failed detection keeps the network detected before the grant: the trigger fires
                // only once, so an unknown network would stick until the next network change.
                observe(::detectNetworkOrNull) { previous, network ->
                    if (previous != network) {
                        notifyListeners(network)
                    }
                }
            }
            return latestObservation.get().network
        }

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
    override fun refreshNetworkStatus(): CurrentNetwork = observe(::detectNetwork)

    /**
     * Stores the network returned by [detect] unless an observation that started later is already
     * stored, then calls [onStored] with the network it replaced. Returns the detected network.
     */
    private inline fun <T : CurrentNetwork?> observe(
        detect: () -> T,
        onStored: (previous: CurrentNetwork, network: CurrentNetwork) -> Unit = { _, _ -> },
    ): T {
        val sequence = observationSequence.incrementAndGet()
        val network = detect()
        if (network != null) {
            var latest = latestObservation.get()
            while (latest.sequence <= sequence) {
                if (latestObservation.compareAndSet(latest, Observation(sequence, network))) {
                    // A shared lock around detection, storage, and notification would serialize
                    // concurrent observations, but a permission-triggered read from a span/log on
                    // the main thread could then block behind a callback's detection and
                    // synchronous listeners. We accept instead that the sequence check only keeps
                    // the cache correct: a thread can pause between storing and notifying, so
                    // listeners can see two overlapping observations out of order. This is rarer
                    // than, and less harmful than, blocking the main thread.
                    onStored(latest.network, network)
                    break
                }
                latest = latestObservation.get()
            }
        }
        return network
    }

    private fun detectNetwork(): CurrentNetwork = detectNetworkOrNull() ?: CurrentNetworkProvider.UNKNOWN_NETWORK

    private fun detectNetworkOrNull(): CurrentNetwork? =
        try {
            networkDetector.detectCurrentNetwork()
        } catch (e: Exception) {
            // guard against security issues/bugs when accessing the Android connectivityManager.
            // see: https://issuetracker.google.com/issues/175055271
            null
        }

    override fun addNetworkChangeListener(listener: NetworkChangeListener) {
        listeners.add(listener)
    }

    override fun removeNetworkChangeListener(listener: NetworkChangeListener) {
        listeners.remove(listener)
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
            val activeNetwork = observe(::detectNetwork) { _, stored -> notifyListeners(stored) }
            Log.d(RumConstants.OTEL_RUM_LOG_TAG, "  onAvailable: currentNetwork=$activeNetwork")
        }

        override fun onLost(network: Network) {
            // it seems that the "currentNetwork" is still the one that is being lost, so for
            // this method, we'll force it to be NO_NETWORK, rather than relying on the
            // ConnectivityManager to have the right
            // state at the right time during this event.
            val currentNetwork =
                observe({ CurrentNetworkProvider.NO_NETWORK }) { _, stored -> notifyListeners(stored) }
            Log.d(RumConstants.OTEL_RUM_LOG_TAG, "  onLost: currentNetwork=$currentNetwork")
        }
    }

    private class Observation(
        val sequence: Long,
        val network: CurrentNetwork,
    )

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
