/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.data;

import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NetworkConnectionTypeValues;

public enum NetworkState {
    NO_NETWORK_AVAILABLE(NetworkConnectionTypeValues.UNAVAILABLE),
    TRANSPORT_CELLULAR(NetworkConnectionTypeValues.CELL),
    TRANSPORT_WIFI(NetworkConnectionTypeValues.WIFI),
    TRANSPORT_UNKNOWN(NetworkConnectionTypeValues.UNKNOWN),
    // this one doesn't seem to have an otel value at this point.
    TRANSPORT_VPN("vpn");

    private final String humanName;

    NetworkState(String humanName) {
        this.humanName = humanName;
    }

    String getHumanName() {
        return humanName;
    }
}
