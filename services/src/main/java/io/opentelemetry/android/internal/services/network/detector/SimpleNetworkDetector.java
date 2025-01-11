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
import androidx.annotation.RequiresApi;
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return detectUsingModernApi();
        } else {
            return detectUsingLegacyApi();
        }
    }

    @RequiresApi(api = android.os.Build.VERSION_CODES.Q)
    private CurrentNetwork detectUsingModernApi() {
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return NO_NETWORK;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return UNKNOWN_NETWORK;
        }

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return buildCurrentNetwork(NetworkState.TRANSPORT_CELLULAR, "");
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return buildCurrentNetwork(NetworkState.TRANSPORT_WIFI, "");
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            return buildCurrentNetwork(NetworkState.TRANSPORT_VPN, "");
        }
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return buildCurrentNetwork(NetworkState.TRANSPORT_WIRED, "");
        }

        return UNKNOWN_NETWORK;
    }

    @SuppressWarnings("deprecation")
    private CurrentNetwork detectUsingLegacyApi() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null) {
            return NO_NETWORK;
        }

        switch (activeNetwork.getType()) {
            case ConnectivityManager.TYPE_MOBILE:
                return buildCurrentNetwork(
                        NetworkState.TRANSPORT_CELLULAR, activeNetwork.getSubtypeName());
            case ConnectivityManager.TYPE_WIFI:
                return buildCurrentNetwork(
                        NetworkState.TRANSPORT_WIFI, activeNetwork.getSubtypeName());
            case ConnectivityManager.TYPE_VPN:
                return buildCurrentNetwork(
                        NetworkState.TRANSPORT_VPN, activeNetwork.getSubtypeName());
            case ConnectivityManager.TYPE_ETHERNET:
                return buildCurrentNetwork(
                        NetworkState.TRANSPORT_WIRED, activeNetwork.getSubtypeName());
            default:
                return UNKNOWN_NETWORK;
        }
    }

    private CurrentNetwork buildCurrentNetwork(NetworkState state, String subType) {
        return CurrentNetwork.builder(state).subType(subType).build();
    }
}
