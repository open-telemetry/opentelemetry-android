/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal.instrumentation.network;

import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

enum NetworkState {
    NO_NETWORK_AVAILABLE(SemanticAttributes.NetHostConnectionTypeValues.UNAVAILABLE),
    TRANSPORT_CELLULAR(SemanticAttributes.NetHostConnectionTypeValues.CELL),
    TRANSPORT_WIFI(SemanticAttributes.NetHostConnectionTypeValues.WIFI),
    TRANSPORT_UNKNOWN(SemanticAttributes.NetHostConnectionTypeValues.UNKNOWN),
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
