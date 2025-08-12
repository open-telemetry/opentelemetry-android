/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.common.internal.features.networkattributes.data

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
data class Carrier
    @JvmOverloads
    constructor(
        val id: Int = -1,
        val name: String? = null,
        val mobileCountryCode: String? = null,
        val mobileNetworkCode: String? = null,
        val isoCountryCode: String? = null,
    )
