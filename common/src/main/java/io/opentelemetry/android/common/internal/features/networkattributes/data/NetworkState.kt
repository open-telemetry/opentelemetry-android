/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattributes.data

import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NetworkConnectionTypeValues.CELL
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NetworkConnectionTypeValues.UNAVAILABLE
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NetworkConnectionTypeValues.UNKNOWN
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NetworkConnectionTypeValues.WIFI
import io.opentelemetry.kotlin.semconv.NetworkAttributes.NetworkConnectionTypeValues.WIRED

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
enum class NetworkState(
    val humanName: String,
) {
    @OptIn(IncubatingApi::class)
    NO_NETWORK_AVAILABLE(UNAVAILABLE.value),

    @OptIn(IncubatingApi::class)
    TRANSPORT_CELLULAR(CELL.value),

    @OptIn(IncubatingApi::class)
    TRANSPORT_WIFI(WIFI.value),

    @OptIn(IncubatingApi::class)
    TRANSPORT_WIRED(WIRED.value),

    @OptIn(IncubatingApi::class)
    TRANSPORT_UNKNOWN(UNKNOWN.value),

    // this one doesn't seem to have an otel value at this point.
    TRANSPORT_VPN("vpn"),
}
