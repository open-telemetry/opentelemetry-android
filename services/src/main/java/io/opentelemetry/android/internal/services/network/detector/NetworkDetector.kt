/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network.detector

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import io.opentelemetry.android.common.internal.features.networkattributes.data.CurrentNetwork
import io.opentelemetry.android.internal.services.network.CarrierFinder

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
interface NetworkDetector {
    fun detectCurrentNetwork(): CurrentNetwork

    companion object {
        @JvmStatic
        fun create(context: Context): NetworkDetector {
            // TODO: Use ServiceManager to get the ConnectivityManager or similar (not yet managed/abstracted)
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // TODO: Use ServiceManager to get the TelephonyManager or similar (not yet managed/abstracted)
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val carrierFinder = CarrierFinder(telephonyManager)
                return PostApi28NetworkDetector(
                    connectivityManager,
                    telephonyManager,
                    carrierFinder,
                    context,
                )
            }
            return SimpleNetworkDetector(connectivityManager)
        }
    }
}
