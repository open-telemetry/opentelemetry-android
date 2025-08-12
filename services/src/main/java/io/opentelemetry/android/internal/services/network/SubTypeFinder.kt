/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import io.opentelemetry.android.common.RumConstants

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
class SubTypeFinder(
    private val context: Context,
    private val telephonyManager: TelephonyManager,
) {
    /**
     * Gets the current cellular network subtype with proper permission and API level handling.
     *
     * @return Network subtype name or null if unavailable or permission denied
     */
    @SuppressLint("MissingPermission", "deprecated")
    fun get(): String? {
        if (!NetworkUtils.hasReadPhoneStatePermission(context)) {
            return null
        }

        return try {
            val networkType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    getNetworkTypePostApi24()
                } else {
                    telephonyManager.networkType
                }
            NetworkUtils.getNetworkTypeName(networkType)
        } catch (e: SecurityException) {
            Log.w(
                RumConstants.OTEL_RUM_LOG_TAG,
                "SecurityException when accessing network type",
                e,
            )
            null
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun getNetworkTypePostApi24(): Int = telephonyManager.dataNetworkType
}
