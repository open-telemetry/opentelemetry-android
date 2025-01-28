/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState;
import io.opentelemetry.android.internal.services.network.detector.NetworkDetector;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

// note: based on ideas from stack overflow:
// https://stackoverflow.com/questions/32547006/connectivitymanager-getnetworkinfoint-deprecated

/**
 * A provider of {@link CurrentNetwork} information. Registers itself in the Android {@link
 * ConnectivityManager} and listens for network changes.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class CurrentNetworkProvider {

    public static final CurrentNetwork NO_NETWORK =
            CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build();
    public static final CurrentNetwork UNKNOWN_NETWORK =
            CurrentNetwork.builder(NetworkState.TRANSPORT_UNKNOWN).build();

    private final NetworkDetector networkDetector;
    private final ConnectivityManager connectivityManager;

    private volatile CurrentNetwork currentNetwork = UNKNOWN_NETWORK;
    private final List<NetworkChangeListener> listeners = new CopyOnWriteArrayList<>();

    public CurrentNetworkProvider(
            NetworkDetector networkDetector, ConnectivityManager connectivityManager) {
        this(
                networkDetector,
                connectivityManager,
                CurrentNetworkProvider::createNetworkMonitoringRequest);
    }

    CurrentNetworkProvider(
            NetworkDetector networkDetector,
            ConnectivityManager connectivityManager,
            Supplier<NetworkRequest> createNetworkMonitoringRequest) {
        this.connectivityManager = connectivityManager;
        this.networkDetector = networkDetector;
        startMonitoring(createNetworkMonitoringRequest);
    }

    private void startMonitoring(Supplier<NetworkRequest> createNetworkMonitoringRequest) {
        refreshNetworkStatus();
        try {
            registerNetworkCallbacks(createNetworkMonitoringRequest);
        } catch (Exception e) {
            // if this fails, we'll go without network change events.
            Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "Failed to register network callbacks. Automatic network monitoring is disabled.",
                    e);
        }
    }

    private void registerNetworkCallbacks(Supplier<NetworkRequest> createNetworkMonitoringRequest) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerNetworkCallbackApi24();
        } else {
            NetworkRequest networkRequest = createNetworkMonitoringRequest.get();
            connectivityManager.registerNetworkCallback(networkRequest, new ConnectionMonitor());
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void registerNetworkCallbackApi24() {
        connectivityManager.registerDefaultNetworkCallback(new ConnectionMonitor());
    }

    /** Returns up-to-date {@linkplain CurrentNetwork current network information}. */
    public CurrentNetwork refreshNetworkStatus() {
        try {
            currentNetwork = networkDetector.detectCurrentNetwork();
        } catch (Exception e) {
            // guard against security issues/bugs when accessing the Android connectivityManager.
            // see: https://issuetracker.google.com/issues/175055271
            currentNetwork = UNKNOWN_NETWORK;
        }
        return currentNetwork;
    }

    private static NetworkRequest createNetworkMonitoringRequest() {
        // note: this throws an NPE when running in junit without robolectric, due to Android
        return new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build();
    }

    public CurrentNetwork getCurrentNetwork() {
        return currentNetwork;
    }

    public void addNetworkChangeListener(NetworkChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners(CurrentNetwork activeNetwork) {
        for (NetworkChangeListener listener : listeners) {
            listener.onNetworkChange(activeNetwork);
        }
    }

    private final class ConnectionMonitor extends ConnectivityManager.NetworkCallback {

        @Override
        public void onAvailable(@NonNull Network network) {
            CurrentNetwork activeNetwork = refreshNetworkStatus();
            Log.d(RumConstants.OTEL_RUM_LOG_TAG, "  onAvailable: currentNetwork=" + activeNetwork);

            notifyListeners(activeNetwork);
        }

        @Override
        public void onLost(@NonNull Network network) {
            // it seems that the "currentNetwork" is still the one that is being lost, so for
            // this method, we'll force it to be NO_NETWORK, rather than relying on the
            // ConnectivityManager to have the right
            // state at the right time during this event.
            CurrentNetwork currentNetwork = NO_NETWORK;
            CurrentNetworkProvider.this.currentNetwork = currentNetwork;
            Log.d(RumConstants.OTEL_RUM_LOG_TAG, "  onLost: currentNetwork=" + currentNetwork);

            notifyListeners(currentNetwork);
        }
    }
}
