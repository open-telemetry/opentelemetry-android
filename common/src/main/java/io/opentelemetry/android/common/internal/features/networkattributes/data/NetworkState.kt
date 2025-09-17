/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattributes.data

import io.opentelemetry.semconv.incubating.NetworkIncubatingAttributes.NetworkConnectionTypeIncubatingValues

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
enum class NetworkState(
    val humanName: String,
) {
    NO_NETWORK_AVAILABLE(NetworkConnectionTypeIncubatingValues.UNAVAILABLE),
    TRANSPORT_CELLULAR(NetworkConnectionTypeIncubatingValues.CELL),
    TRANSPORT_WIFI(NetworkConnectionTypeIncubatingValues.WIFI),
    TRANSPORT_WIRED(NetworkConnectionTypeIncubatingValues.WIRED),
    TRANSPORT_UNKNOWN(NetworkConnectionTypeIncubatingValues.UNKNOWN),

    // this one doesn't seem to have an otel value at this point.
    TRANSPORT_VPN("vpn"),
}
