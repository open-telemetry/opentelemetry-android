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

import static com.splunk.rum.ConnectionUtil.NO_NETWORK;
import static com.splunk.rum.ConnectionUtil.UNKNOWN_NETWORK;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

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
