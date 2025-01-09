/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector;

import static io.opentelemetry.android.internal.services.network.CurrentNetworkProvider.NO_NETWORK;
import static io.opentelemetry.android.internal.services.network.CurrentNetworkProvider.UNKNOWN_NETWORK;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork;
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState;

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
class SimpleNetworkDetector implements NetworkDetector {
    private final ConnectivityManager connectivityManager;

    SimpleNetworkDetector(ConnectivityManager connectivityManager) {
        this.connectivityManager = connectivityManager;
    }

    @Override
    public CurrentNetwork detectCurrentNetwork() {
        // For API 29 and above, use modern APIs
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return NO_NETWORK;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return UNKNOWN_NETWORK;
            }

            // Determine network type based on transport capabilities
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                        .subType("") // Additional details can be added
                        .build();
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).subType("").build();
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return CurrentNetwork.builder(NetworkState.TRANSPORT_VPN).subType("").build();
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return CurrentNetwork.builder(NetworkState.TRANSPORT_WIRED).subType("").build();
            }

            // Default to UNKNOWN_NETWORK for other types
            return UNKNOWN_NETWORK;
        }

        // For API 28 and below, use deprecated methods
        NetworkInfo activeNetwork =
                connectivityManager.getActiveNetworkInfo(); // Deprecated in API 29
        if (activeNetwork == null) {
            return NO_NETWORK;
        }

        // Determine network type using TYPE_* constants
        switch (activeNetwork.getType()) { // Deprecated in API 28
            case ConnectivityManager.TYPE_MOBILE:
                return CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                        .subType(activeNetwork.getSubtypeName())
                        .build();
            case ConnectivityManager.TYPE_WIFI:
                return CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI)
                        .subType(activeNetwork.getSubtypeName())
                        .build();
            case ConnectivityManager.TYPE_VPN:
                return CurrentNetwork.builder(NetworkState.TRANSPORT_VPN)
                        .subType(activeNetwork.getSubtypeName())
                        .build();
            case ConnectivityManager.TYPE_ETHERNET:
                return CurrentNetwork.builder(NetworkState.TRANSPORT_WIRED)
                        .subType(activeNetwork.getSubtypeName())
                        .build();
        }
        // Return UNKNOWN_NETWORK if type does not match predefined cases
        return UNKNOWN_NETWORK;
    }
}
