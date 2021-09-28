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

package com.splunk.rum;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.function.Supplier;

//note: based on ideas from stack overflow: https://stackoverflow.com/questions/32547006/connectivitymanager-getnetworkinfoint-deprecated
class ConnectionUtil {

    static final CurrentNetwork NO_NETWORK = new CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE, null);
    static final CurrentNetwork UNKNOWN_NETWORK = new CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN, null);

    private final NetworkDetector networkDetector;

    private volatile CurrentNetwork currentNetwork;
    private volatile ConnectionStateListener connectionStateListener;

    ConnectionUtil(NetworkDetector networkDetector) {
        this.networkDetector = networkDetector;
    }

    void startMonitoring(Supplier<NetworkRequest> createNetworkMonitoringRequest, ConnectivityManager connectivityManager) {
        refreshNetworkStatus();
        try {
            registerNetworkCallbacks(createNetworkMonitoringRequest, connectivityManager);
        } catch (Exception e) {
            //if this fails, we'll go without network change events.
            Log.w(SplunkRum.LOG_TAG, "Failed to register network callbacks. Automatic network monitoring is disabled.", e);
        }
    }

    private void registerNetworkCallbacks(Supplier<NetworkRequest> createNetworkMonitoringRequest, ConnectivityManager connectivityManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(new ConnectionMonitor());
        } else {
            NetworkRequest networkRequest = createNetworkMonitoringRequest.get();
            connectivityManager.registerNetworkCallback(networkRequest, new ConnectionMonitor());
        }
    }

    CurrentNetwork refreshNetworkStatus() {
        try {
            currentNetwork = networkDetector.detectCurrentNetwork();
        } catch (Exception e) {
            //guard against security issues/bugs when accessing the Android connectivityManager.
            // see: https://issuetracker.google.com/issues/175055271
            currentNetwork = UNKNOWN_NETWORK;
        }
        return currentNetwork;
    }

    static NetworkRequest createNetworkMonitoringRequest() {
        //note: this throws an NPE when running in junit without robolectric, due to Android
        return new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build();
    }

    boolean isOnline() {
        return currentNetwork.isOnline();
    }

    CurrentNetwork getActiveNetwork() {
        return currentNetwork;
    }

    void setInternetStateListener(ConnectionStateListener listener) {
        connectionStateListener = listener;
    }

    private class ConnectionMonitor extends ConnectivityManager.NetworkCallback {

        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(SplunkRum.LOG_TAG, "onAvailable: ");
            CurrentNetwork activeNetwork = refreshNetworkStatus();
            if (connectionStateListener != null) {
                connectionStateListener.onAvailable(true, activeNetwork);
                Log.d(SplunkRum.LOG_TAG, "  onAvailable: isConnected:" + isOnline() + ", activeNetwork: " + activeNetwork);
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            Log.d(SplunkRum.LOG_TAG, "onLost: ");
            //it seems that the "currentActiveNetwork" is still the one that is being lost, so for
            //this method, we'll force it to be NO_NETWORK, rather than relying on the ConnectivityManager to have the right
            //state at the right time during this event.
            CurrentNetwork activeNetwork = NO_NETWORK;
            currentNetwork = activeNetwork;
            if (connectionStateListener != null) {
                connectionStateListener.onAvailable(false, activeNetwork);
                Log.d(SplunkRum.LOG_TAG, "  onLost: isConnected:" + false + ", activeNetwork: " + activeNetwork);
            }
        }
    }
}