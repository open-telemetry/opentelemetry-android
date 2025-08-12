/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

/**
 * Shared utilities for network detection.
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
object NetworkUtils {
    /**
     * Checks if the app has READ_PHONE_STATE permission.
     */
    fun hasReadPhoneStatePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Maps TelephonyManager network type constants to human-readable strings.
     */
    fun getNetworkTypeName(networkType: Int): String =
        getGsmNetworkTypeName(networkType)
            ?: getCdmaNetworkTypeName(networkType)
            ?: getLteAndModernNetworkTypeName(networkType)
            ?: "UNKNOWN"

    private fun getGsmNetworkTypeName(networkType: Int): String? =
        when (networkType) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
            else -> null
        }

    @Suppress("DEPRECATION")
    private fun getCdmaNetworkTypeName(networkType: Int): String? =
        when (networkType) {
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
            TelephonyManager.NETWORK_TYPE_IDEN,
            -> "IDEN"
            else -> null
        }

    private fun getLteAndModernNetworkTypeName(networkType: Int): String? =
        when (networkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            TelephonyManager.NETWORK_TYPE_NR -> "NR"
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> "UNKNOWN"
            else -> null
        }

    /**
     * Checks if a CharSequence is valid (not null or empty).
     */
    fun isValidString(str: CharSequence?): Boolean = !str.isNullOrEmpty()
}
