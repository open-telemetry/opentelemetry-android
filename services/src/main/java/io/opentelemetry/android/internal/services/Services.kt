/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ProcessLifecycleOwner
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleImpl
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateWatcher
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.CurrentNetworkProviderImpl
import io.opentelemetry.android.internal.services.network.detector.NetworkDetector
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWork
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkImpl
import io.opentelemetry.android.internal.services.storage.CacheStorage
import io.opentelemetry.android.internal.services.storage.CacheStorageImpl
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTrackerImpl

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
class Services internal constructor(
    private val context: Context,
) : ServicesFactory {
    override val cacheStorage: CacheStorage by lazy {
        CacheStorageImpl(context)
    }

    override val periodicWork: PeriodicWork by lazy {
        PeriodicWorkImpl()
    }

    override val currentNetworkProvider: CurrentNetworkProvider by lazy {
        CurrentNetworkProviderImpl(
            NetworkDetector.Companion.create(context),
            context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager,
        )
    }

    override val appLifecycle: AppLifecycle by lazy {
        AppLifecycleImpl(
            ApplicationStateWatcher(),
            ProcessLifecycleOwner.Companion.get().lifecycle,
        )
    }

    override val visibleScreenTracker: VisibleScreenTracker by lazy {
        VisibleScreenTrackerImpl(context)
    }

    override fun close() {
        cacheStorage.close()
        periodicWork.close()
        currentNetworkProvider.close()
        appLifecycle.close()
        visibleScreenTracker.close()
    }

    companion object {
        @SuppressLint("StaticFieldLeak") // ignoring, we're using the application context
        private var instance: Services? = null

        @JvmStatic
        fun get(context: Context): Services =
            synchronized(this) {
                if (instance == null) {
                    set(Services(context))
                }
                return checkNotNull(instance)
            }

        @JvmStatic
        @VisibleForTesting
        fun set(services: Services?) {
            instance = services
        }
    }
}
