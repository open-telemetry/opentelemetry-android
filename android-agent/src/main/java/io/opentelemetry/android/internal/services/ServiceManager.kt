/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import android.app.Application
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleService
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
interface ServiceManager : Startable {
    fun getPreferences(): Preferences

    fun getCacheStorage(): CacheStorage

    fun getPeriodicWorkService(): PeriodicWorkService

    fun getCurrentNetworkProvider(): CurrentNetworkProvider

    fun getAppLifecycleService(): AppLifecycleService

    fun getVisibleScreenService(): VisibleScreenService

    companion object {
        private var instance: ServiceManager? = null

        @JvmStatic
        fun initialize(application: Application) {
            if (instance != null) {
                return
            }
            instance =
                ServiceManagerImpl(
                    listOf(
                        Preferences.create(application),
                        CacheStorage(
                            application,
                        ),
                        PeriodicWorkService(),
                        CurrentNetworkProvider.create(application),
                        AppLifecycleService.create(),
                        VisibleScreenService.create(application),
                    ),
                )
        }

        @JvmStatic
        fun get(): ServiceManager {
            checkNotNull(instance) { "Services haven't been initialized" }
            return instance!!
        }
    }
}
