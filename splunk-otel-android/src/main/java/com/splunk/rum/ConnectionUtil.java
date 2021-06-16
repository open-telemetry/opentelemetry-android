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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

//note: based on ideas from stack overflow: https://stackoverflow.com/questions/32547006/connectivitymanager-getnetworkinfoint-deprecated
class ConnectionUtil {

    static final CurrentNetwork NO_NETWORK = new CurrentNetwork(NetworkState.NO_NETWORK_AVAILABLE, null);
    static final CurrentNetwork UNKNOWN_NETWORK = new CurrentNetwork(NetworkState.TRANSPORT_UNKNOWN, null);

    private final ConnectionMonitor connectionMonitor;
    private final NetworkDetector networkDetector;

    private final AtomicReference<CurrentNetwork> currentNetwork = new AtomicReference<>();

    ConnectionUtil(Context context) {
        this(ConnectionUtil::createNetworkMonitoringRequest, NetworkDetector.create(context), (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    //for testing, since building the NetworkRequest fails in junit due to .. Android.
    ConnectionUtil(Supplier<NetworkRequest> createNetworkMonitoringRequest, NetworkDetector networkDetector, ConnectivityManager connectivityManager) {
        this.connectionMonitor = new ConnectionMonitor();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(connectionMonitor);
        } else {
            NetworkRequest networkRequest = createNetworkMonitoringRequest.get();
            connectivityManager.registerNetworkCallback(networkRequest, connectionMonitor);
        }
        this.networkDetector = networkDetector;
        refreshNetworkStatus();
    }

    CurrentNetwork refreshNetworkStatus() {
        CurrentNetwork activeNetwork = networkDetector.detectCurrentNetwork();
        currentNetwork.set(activeNetwork);
        return activeNetwork;
    }

    private static NetworkRequest createNetworkMonitoringRequest() {
        //todo: this throws an NPE when running in junit. what's up with that?
        return new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_BLUETOOTH)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .build();
    }

    boolean isOnline() {
        return currentNetwork.get().isOnline();
    }

    CurrentNetwork getActiveNetwork() {
        return currentNetwork.get();
    }

    void setInternetStateListener(ConnectionStateListener listener) {
        connectionMonitor.setOnConnectionStateListener(listener);
    }

    class ConnectionMonitor extends ConnectivityManager.NetworkCallback {

        private ConnectionStateListener connectionStateListener;

        void setOnConnectionStateListener(ConnectionStateListener connectionStateListener) {
            this.connectionStateListener = connectionStateListener;
        }

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
            currentNetwork.set(activeNetwork);
            if (connectionStateListener != null) {
                connectionStateListener.onAvailable(false, activeNetwork);
                Log.d(SplunkRum.LOG_TAG, "  onLost: isConnected:" + false + ", activeNetwork: " + activeNetwork);
            }
        }
    }
}