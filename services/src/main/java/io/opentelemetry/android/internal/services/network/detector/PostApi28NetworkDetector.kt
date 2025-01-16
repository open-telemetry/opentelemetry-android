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
import io.opentelemetry.android.common.internal.features.networkattributes.data.Carrier
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState.TRANSPORT_CELLULAR
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState.TRANSPORT_VPN
import io.opentelemetry.android.common.internal.features.networkattributes.data.NetworkState.TRANSPORT_WIFI
import io.opentelemetry.android.internal.services.network.CarrierFinder
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider.UNKNOWN_NETWORK

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
@RequiresApi(api = Build.VERSION_CODES.P)
//TODO: Refactor tests and remove open
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
        val carrier = carrierFinder.get()

        fun hasTransport(capability: Int): Boolean = capabilities.hasCapability(capability)

        val network = buildFromCarrier(carrier)
        return when {
            hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> withCellDataType(carrier)
            hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> network(TRANSPORT_WIFI)
            hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> network(TRANSPORT_VPN)
            else -> UNKNOWN_NETWORK
        }
    }

    private fun buildFromCarrier(carrier: Carrier): (NetworkState) -> CurrentNetwork =
        { CurrentNetwork.builder(it).carrier(carrier).build() }


    private fun withCellDataType(
        carrier: Carrier
    ): CurrentNetwork {
        val builder = CurrentNetwork.builder(TRANSPORT_CELLULAR)
            .carrier(carrier)
        if (canReadPhoneState()) {
            val dataNetworkType = getDataNetworkTypeName(telephonyManager.dataNetworkType)
            return builder.subType(dataNetworkType).build()
        }
        return builder.build();
    }

    // visible for testing
    open fun canReadPhoneState(): Boolean {
        return (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED)
    }

    private fun getDataNetworkTypeName(dataNetworkType: Int): String {
        return when (dataNetworkType) {
            NETWORK_TYPE_1xRTT -> "1xRTT"
            NETWORK_TYPE_CDMA ->  "CDMA"
            NETWORK_TYPE_EDGE ->  "EDGE"
            NETWORK_TYPE_EHRPD ->  "EHRPD"
            NETWORK_TYPE_EVDO_0 ->  "EVDO_0"
            NETWORK_TYPE_EVDO_A ->  "EVDO_A"
            NETWORK_TYPE_EVDO_B ->  "EVDO_B"
            NETWORK_TYPE_GPRS ->  "GPRS"
            NETWORK_TYPE_GSM ->  "GSM"
            NETWORK_TYPE_HSDPA ->  "HSDPA"
            NETWORK_TYPE_HSPA ->  "HSPA"
            NETWORK_TYPE_HSPAP ->  "HSPAP"
            NETWORK_TYPE_HSUPA ->  "HSUPA"
            NETWORK_TYPE_IDEN ->  "IDEN"
            NETWORK_TYPE_IWLAN ->  "IWLAN"
            NETWORK_TYPE_LTE ->  "LTE"
            NETWORK_TYPE_NR ->  "NR"
            NETWORK_TYPE_TD_SCDMA ->  "SCDMA"
            NETWORK_TYPE_UMTS ->  "UMTS"
            NETWORK_TYPE_UNKNOWN ->  "UNKNOWN"
            else -> "UNKNOWN"
        }
    }

    private fun network(state: NetworkState): CurrentNetwork = CurrentNetwork.builder(state).build()
}
