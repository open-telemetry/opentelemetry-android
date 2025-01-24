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
class Services internal constructor(
    private val factory: ServicesFactory,
) {
    val preferences: Preferences by lazy {
        factory.createPreferences()
    }

    val cacheStorage: CacheStorage by lazy {
        factory.createCacheStorage()
    }

    val periodicWork: PeriodicWork by lazy {
        factory.createPeriodicWork()
    }

    val currentNetworkProvider: CurrentNetworkProvider by lazy {
        factory.createCurrentNetworkProvider()
    }

    val appLifecycle: AppLifecycle by lazy {
        factory.createAppLifecycle()
    }

    val visibleScreenTracker: VisibleScreenTracker by lazy {
        factory.createVisibleScreenTracker()
    }

    companion object {
        private var instance: Services? = null

        @JvmStatic
        fun get(application: Application): Services =
            synchronized(this) {
                if (instance == null) {
                    set(Services(ServicesFactory(application)))
                }
                return instance!!
            }

        // Visible for tests
        @JvmStatic
        fun set(services: Services?) {
            instance = services
        }
    }

    internal class ServicesFactory(
        private val application: Application,
    ) {
        fun createPreferences(): Preferences =
            Preferences(
                application.getSharedPreferences(
                    "io.opentelemetry.android" + ".prefs",
                    Context.MODE_PRIVATE,
                ),
            )

        fun createCacheStorage(): CacheStorage = CacheStorage(application)

        fun createPeriodicWork(): PeriodicWork = PeriodicWork()

        fun createCurrentNetworkProvider(): CurrentNetworkProvider =
            CurrentNetworkProvider(
                NetworkDetector.create(application),
                application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager,
            )

        fun createAppLifecycle(): AppLifecycle =
            AppLifecycle(
                ApplicationStateWatcher(),
                ProcessLifecycleOwner.get().lifecycle,
            )

        fun createVisibleScreenTracker(): VisibleScreenTracker = VisibleScreenTracker(application)
    }
}
