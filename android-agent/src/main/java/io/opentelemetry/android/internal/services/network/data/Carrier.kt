/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.data

import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
@RequiresApi(api = Build.VERSION_CODES.P)
data class Carrier
    @JvmOverloads
    internal constructor(
        val id: Int = TelephonyManager.UNKNOWN_CARRIER_ID,
        val name: String? = null,
        val mobileCountryCode: String? = null,
        val mobileNetworkCode: String? = null,
        val isoCountryCode: String? = null,
    )
