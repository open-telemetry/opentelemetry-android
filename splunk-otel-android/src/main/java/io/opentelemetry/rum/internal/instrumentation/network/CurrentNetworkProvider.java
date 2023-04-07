/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal.instrumentation.network;

import static io.opentelemetry.rum.internal.RumConstants.OTEL_RUM_LOG_TAG;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

// note: based on ideas from stack overflow:
// https://stackoverflow.com/questions/32547006/connectivitymanager-getnetworkinfoint-deprecated

/**
 * A provider of {@link CurrentNetwork} information. Registers itself in the Android {@link
 * ConnectivityManager} and listens for network changes.
 */
public final class CurrentNetworkProvider {

    static final CurrentNetwork NO_NETWORK =
            CurrentNetwork.builder(NetworkState.NO_NETWORK_AVAILABLE).build();
    static final CurrentNetwork UNKNOWN_NETWORK =
            CurrentNetwork.builder(NetworkState.TRANSPORT_UNKNOWN).build();

    /**
     * Creates a new {@link CurrentNetworkProvider} instance and registers network callbacks in the
     * Android {@link ConnectivityManager}.
     */
    public static CurrentNetworkProvider createAndStart(Application application) {
        Context context = application.getApplicationContext();
        CurrentNetworkProvider currentNetworkProvider =
                new CurrentNetworkProvider(NetworkDetector.create(context));
        currentNetworkProvider.startMonitoring(
                CurrentNetworkProvider::createNetworkMonitoringRequest,
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return currentNetworkProvider;
    }

    private final NetworkDetector networkDetector;

    private volatile CurrentNetwork currentNetwork = UNKNOWN_NETWORK;
    private final List<NetworkChangeListener> listeners = new CopyOnWriteArrayList<>();

    // visible for tests
    CurrentNetworkProvider(NetworkDetector networkDetector) {
        this.networkDetector = networkDetector;
    }

    // visible for tests
    void startMonitoring(
            Supplier<NetworkRequest> createNetworkMonitoringRequest,
            ConnectivityManager connectivityManager) {
        refreshNetworkStatus();
        try {
            registerNetworkCallbacks(createNetworkMonitoringRequest, connectivityManager);
        } catch (Exception e) {
            // if this fails, we'll go without network change events.
            Log.w(
                    OTEL_RUM_LOG_TAG,
                    "Failed to register network callbacks. Automatic network monitoring is disabled.",
                    e);
        }
    }

    private void registerNetworkCallbacks(
            Supplier<NetworkRequest> createNetworkMonitoringRequest,
            ConnectivityManager connectivityManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(new ConnectionMonitor());
        } else {
            NetworkRequest networkRequest = createNetworkMonitoringRequest.get();
            connectivityManager.registerNetworkCallback(networkRequest, new ConnectionMonitor());
        }
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

    CurrentNetwork getCurrentNetwork() {
        return currentNetwork;
    }

    void addNetworkChangeListener(NetworkChangeListener listener) {
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
            Log.d(OTEL_RUM_LOG_TAG, "  onAvailable: currentNetwork=" + activeNetwork);

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
            Log.d(OTEL_RUM_LOG_TAG, "  onLost: currentNetwork=" + currentNetwork);

            notifyListeners(currentNetwork);
        }
    }
}
