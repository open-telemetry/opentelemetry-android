/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattributes.data

/**
 * A value class representing the current network that the device is connected to.
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
data class CurrentNetwork
    @JvmOverloads
    constructor(
        val state: NetworkState,
        private val carrier: Carrier? = null,
        val subType: String? = null,
    ) {
        val carrierCountryCode: String?
            get() = carrier?.mobileCountryCode

        val carrierIsoCountryCode: String?
            get() = carrier?.isoCountryCode

        val carrierNetworkCode: String?
            get() = carrier?.mobileNetworkCode

        val carrierName: String?
            get() = carrier?.name
    }
