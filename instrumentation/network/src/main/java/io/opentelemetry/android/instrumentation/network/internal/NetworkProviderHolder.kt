/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.network.internal

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import androidx.annotation.VisibleForTesting
import io.opentelemetry.android.instrumentation.network.internal.detector.NetworkDetector

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
        )
}
