/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.opentelemetry.android.internal.services.network.detector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.internal.services.network.CarrierFinder
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
@RequiresApi(api = Build.VERSION_CODES.P)
internal open class PostApi28NetworkDetector(
    private val connectivityManager: ConnectivityManager,
    private val telephonyManager: TelephonyManager,
    private val carrierFinder: CarrierFinder,
    private val context: Context
) : NetworkDetector {
    @SuppressLint("MissingPermission")
    override fun detectCurrentNetwork(): CurrentNetwork {
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                ?: return CurrentNetworkProvider.NO_NETWORK
        var subType: String? = null
        val carrier = carrierFinder.get()
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            // If the app has the permission, use it to get a subtype.
            if (canReadPhoneState()) {
                subType = getDataNetworkTypeName(telephonyManager.dataNetworkType)
            }
            return CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                .carrier(carrier)
                .subType(subType)
                .build()
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return CurrentNetwork.builder(NetworkState.TRANSPORT_WIFI).carrier(carrier).build()
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            return CurrentNetwork.builder(NetworkState.TRANSPORT_VPN).carrier(carrier).build()
        }
        // there is an active network, but it doesn't fall into the neat buckets above
        return CurrentNetworkProvider.UNKNOWN_NETWORK
    }

    // visible for testing
    open fun canReadPhoneState(): Boolean {
        return (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun getDataNetworkTypeName(dataNetworkType: Int): String {
        when (dataNetworkType) {
            NETWORK_TYPE_1xRTT -> return "1xRTT"
            TelephonyManager.NETWORK_TYPE_CDMA -> return "CDMA"
            TelephonyManager.NETWORK_TYPE_EDGE -> return "EDGE"
            TelephonyManager.NETWORK_TYPE_EHRPD -> return "EHRPD"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> return "EVDO_0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> return "EVDO_A"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> return "EVDO_B"
            TelephonyManager.NETWORK_TYPE_GPRS -> return "GPRS"
            TelephonyManager.NETWORK_TYPE_GSM -> return "GSM"
            TelephonyManager.NETWORK_TYPE_HSDPA -> return "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> return "HSPA"
            TelephonyManager.NETWORK_TYPE_HSPAP -> return "HSPAP"
            TelephonyManager.NETWORK_TYPE_HSUPA -> return "HSUPA"
            TelephonyManager.NETWORK_TYPE_IDEN -> return "IDEN"
            TelephonyManager.NETWORK_TYPE_IWLAN -> return "IWLAN"
            TelephonyManager.NETWORK_TYPE_LTE -> return "LTE"
            TelephonyManager.NETWORK_TYPE_NR -> return "NR"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> return "SCDMA"
            TelephonyManager.NETWORK_TYPE_UMTS -> return "UMTS"
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> return "UNKNOWN"
        }
        return "UNKNOWN"
    }
}
