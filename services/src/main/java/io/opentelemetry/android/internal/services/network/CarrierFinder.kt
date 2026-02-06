/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
internal class CarrierFinder(
    private val context: Context,
    private val telephonyManager: TelephonyManager,
) {
    fun get(): Carrier? {
        if (!hasTelephonyFeature(context)) {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "Cannot determine carrier details: telephony feature missing.",
            )
            return null
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (hasPhoneStatePermission(context)) {
                    return this.getCarrierPostApi28()
                } else {
                    Log.w(
                        RumConstants.OTEL_RUM_LOG_TAG,
                        "Missing read phone state permission, using legacy carrier methods.",
                    )
                    return this.getCarrierPreApi28()
                }
            } else {
                return this.getCarrierPreApi28()
            }
        } catch (e: SecurityException) {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "SecurityException when accessing carrier info",
                e,
            )
        }
        return null
    }

    /** Extracts carrier information using modern APIs (Post API 28).  */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun getCarrierPostApi28(): Carrier {
        val id = telephonyManager.simCarrierId

        var name: String? = null
        val carrierName = telephonyManager.simCarrierIdName
        if (isValidString(carrierName)) {
            name = carrierName.toString()
        }
        val mccMncIso = getMccMncIso()
        return Carrier(
            id = id,
            name = name,
            mobileCountryCode = mccMncIso[0],
            mobileNetworkCode = mccMncIso[1],
            isoCountryCode = mccMncIso[2],
        )
    }

    /** Extracts carrier information using legacy APIs (Pre API 28).  */
    private fun getCarrierPreApi28(): Carrier {
        var name: String? = null
        var carrierName = telephonyManager.simOperatorName
        if (isValidString(carrierName)) {
            name = carrierName
        } else {
            carrierName = telephonyManager.networkOperatorName
            if (isValidString(carrierName)) {
                name = carrierName
            }
        }
        val mccMncIso = getMccMncIso()
        return Carrier(
            name = name,
            mobileCountryCode = mccMncIso[0],
            mobileNetworkCode = mccMncIso[1],
            isoCountryCode = mccMncIso[2],
        )
    }

    /**
     * Extracts MCC, MNC, and ISO country code from TelephonyManager.
     *
     * @return String array: [mcc, mnc, iso]
     */
    private fun getMccMncIso(): List<String?> =
        run {
            var mcc: String? = null
            var mnc: String? = null
            var iso: String? = null
            val simOperator = telephonyManager.simOperator
            if (isValidString(simOperator) && simOperator.length >= 5) {
                mcc = simOperator.take(3)
                mnc = simOperator.substring(3)
            }
            val isoCountryCode = telephonyManager.simCountryIso
            if (isValidString(isoCountryCode)) {
                iso = isoCountryCode
            }
            listOf(mcc, mnc, iso)
        }
}
