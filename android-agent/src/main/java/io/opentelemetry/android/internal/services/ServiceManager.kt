/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import android.content.Context
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
interface ServiceManager : Startable {
    fun getPreferencesService(): PreferencesService

    fun getCacheStorage(): CacheStorage

    fun getPeriodicWorkService(): PeriodicWorkService

    companion object {
        private var instance: ServiceManager? = null

        @JvmStatic
        fun initialize(appContext: Context) {
            if (instance != null) {
                return
            }
            instance =
                ServiceManagerImpl(
                    listOf(
                        PreferencesService.create(appContext),
                        CacheStorage(
                            appContext,
                        ),
                        PeriodicWorkService(),
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
