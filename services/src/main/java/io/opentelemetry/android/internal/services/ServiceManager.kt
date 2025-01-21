/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ProcessLifecycleOwner
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleService
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateWatcher
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.detector.NetworkDetector
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
class ServiceManager(
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

    val periodicWorkService: PeriodicWorkService by lazy {
        PeriodicWorkService()
    }

    val currentNetworkProvider: CurrentNetworkProvider by lazy {
        CurrentNetworkProvider(
            NetworkDetector.create(application),
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
        )
    }

    val appLifecycleService: AppLifecycleService by lazy {
        AppLifecycleService(
            ApplicationStateWatcher(),
            ProcessLifecycleOwner.get().lifecycle,
        )
    }

    val visibleScreenService: VisibleScreenService by lazy {
        VisibleScreenService(application)
    }

    companion object {
        private var instance: ServiceManager? = null

        @JvmStatic
        fun initialize(application: Application): ServiceManager =
            synchronized(this) {
                if (instance == null) {
                    set(ServiceManager(application))
                }
                return get()
            }

        fun set(serviceManager: ServiceManager) =
            synchronized(this) {
                instance = serviceManager
            }

        @JvmStatic
        fun get(): ServiceManager =
            synchronized(this) {
                return instance!!
            }
    }
}
