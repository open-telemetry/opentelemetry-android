/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ProcessLifecycleOwner
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateWatcher
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.detector.NetworkDetector
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWork
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
class Services(
    private val application: Application,
) {
    val preferences: Preferences by lazy {
        Preferences(
            application.getSharedPreferences(
                "io.opentelemetry.android" + ".prefs",
                Context.MODE_PRIVATE,
            ),
        )
    }

    val cacheStorage: CacheStorage by lazy {
        CacheStorage(application)
    }

    val periodicWork: PeriodicWork by lazy {
        PeriodicWork()
    }

    val currentNetworkProvider: CurrentNetworkProvider by lazy {
        CurrentNetworkProvider(
            NetworkDetector.create(application),
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
        )
    }

    val appLifecycle: AppLifecycle by lazy {
        AppLifecycle(
            ApplicationStateWatcher(),
            ProcessLifecycleOwner.get().lifecycle,
        )
    }

    val visibleScreenTracker: VisibleScreenTracker by lazy {
        VisibleScreenTracker(application)
    }

    companion object {
        private var instance: Services? = null

        @JvmStatic
        fun initialize(application: Application): Services =
            synchronized(this) {
                if (instance == null) {
                    set(Services(application))
                }
                return get()
            }

        fun set(services: Services) =
            synchronized(this) {
                instance = services
            }

        @JvmStatic
        fun get(): Services =
            synchronized(this) {
                return instance!!
            }
    }
}
