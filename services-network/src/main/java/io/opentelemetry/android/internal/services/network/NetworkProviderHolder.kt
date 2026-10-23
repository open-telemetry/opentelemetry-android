/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import androidx.annotation.VisibleForTesting

/**
 * Process-wide lazy holder for the [CurrentNetworkProvider].
 *
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
object NetworkProviderHolder {
    @SuppressLint("StaticFieldLeak") // only application context
    @Volatile
    private var instance: CurrentNetworkProvider? = null

    @JvmStatic
    fun get(context: Context): CurrentNetworkProvider =
        instance ?: synchronized(this) {
            instance ?: createProvider(context).also { instance = it }
        }

    @JvmStatic
    fun close() {
        synchronized(this) {
            instance?.close()
            instance = null
        }
    }

    @JvmStatic
    @VisibleForTesting
    fun set(provider: CurrentNetworkProvider?) {
        synchronized(this) {
            instance = provider
        }
    }

    private fun createProvider(context: Context): CurrentNetworkProvider =
        CurrentNetworkProviderImpl(
            NetworkDetector.create(context),
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
            phoneStatePermissionGrantCheckTrigger(context),
        )

    private fun phoneStatePermissionGrantCheckTrigger(context: Context): PermissionGrantCheckTrigger =
        if (hasTelephonyFeature(context) && !hasPhoneStatePermission(context) && requestsReadPhoneState(context)) {
            // READ_PHONE_STATE is the only phone state permission that can be granted later.
            PollingPermissionGrantCheckTrigger({ hasReadPhoneStatePermission(context) })
        } else {
            // No telephony, the permission is already granted, or the app never requests it.
            PermissionGrantCheckTrigger.NONE
        }
}
