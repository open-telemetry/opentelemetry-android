/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network;

import io.opentelemetry.semconv.SemanticAttributes;

enum NetworkState {
    NO_NETWORK_AVAILABLE(SemanticAttributes.NetworkConnectionTypeValues.UNAVAILABLE),
    TRANSPORT_CELLULAR(SemanticAttributes.NetworkConnectionTypeValues.CELL),
    TRANSPORT_WIFI(SemanticAttributes.NetworkConnectionTypeValues.WIFI),
    TRANSPORT_UNKNOWN(SemanticAttributes.NetworkConnectionTypeValues.UNKNOWN),
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
