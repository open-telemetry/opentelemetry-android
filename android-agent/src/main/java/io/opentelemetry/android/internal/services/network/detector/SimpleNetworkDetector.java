/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector;

import static io.opentelemetry.android.internal.services.network.CurrentNetworkProvider.NO_NETWORK;
import static io.opentelemetry.android.internal.services.network.CurrentNetworkProvider.UNKNOWN_NETWORK;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import io.opentelemetry.android.internal.services.network.data.CurrentNetwork;
import io.opentelemetry.android.internal.services.network.data.NetworkState;

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
        NetworkInfo activeNetwork =
                connectivityManager.getActiveNetworkInfo(); // Deprecated in API 29
        if (activeNetwork == null) {
            return NO_NETWORK;
        }
        switch (activeNetwork.getType()) {
            case ConnectivityManager.TYPE_MOBILE: // Deprecated in API 28
                return CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                        .subType(activeNetwork.getSubtypeName())
                        .build();
            case ConnectivityManager.TYPE_WIFI: // Deprecated in API 28
                return CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI)
                        .subType(activeNetwork.getSubtypeName())
                        .build();
            case ConnectivityManager.TYPE_VPN:
                return CurrentNetwork.builder(NetworkState.TRANSPORT_VPN)
                        .subType(activeNetwork.getSubtypeName())
                        .build();
        }
        // there is an active network, but it doesn't fall into the neat buckets above
        return UNKNOWN_NETWORK;
    }
}
