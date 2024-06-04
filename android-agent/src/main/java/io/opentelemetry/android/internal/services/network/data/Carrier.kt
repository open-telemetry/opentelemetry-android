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
data class Carrier internal constructor(
    val id: Int,
    val name: String?,
    val mobileCountryCode: String?,
    val mobileNetworkCode: String?,
    val isoCountryCode: String?,
) {
    constructor(builder: Builder) : this(
        id = builder.id,
        name = builder.name,
        mobileCountryCode = builder.mobileCountryCode,
        mobileNetworkCode = builder.mobileNetworkCode,
        isoCountryCode = builder.isoCountryCode,
    )

    class Builder {
        internal var id: Int = TelephonyManager.UNKNOWN_CARRIER_ID
        var name: String? = null
        var mobileCountryCode: String? = null
        var mobileNetworkCode: String? = null
        var isoCountryCode: String? = null

        fun build(): Carrier {
            return Carrier(this)
        }

        fun id(id: Int): Builder {
            this.id = id
            return this
        }

        fun name(name: String?): Builder {
            this.name = name
            return this
        }

        fun mobileCountryCode(countryCode: String?): Builder {
            this.mobileCountryCode = countryCode
            return this
        }

        fun mobileNetworkCode(networkCode: String?): Builder {
            this.mobileNetworkCode = networkCode
            return this
        }

        fun isoCountryCode(isoCountryCode: String?): Builder {
            this.isoCountryCode = isoCountryCode
            return this
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
