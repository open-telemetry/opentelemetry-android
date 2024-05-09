/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.squareup.wire.internal.toUnmodifiableMap
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
internal class ServiceManager
    @VisibleForTesting
    internal constructor(services: List<Any>) :
    Startable {
        private val services: Map<Class<out Any>, Any>

        init {
            val map: MutableMap<Class<out Any>, Any> = HashMap()
            for (service in services) {
                map[service.javaClass] = service
            }
            this.services = map.toUnmodifiableMap()
        }

        override fun start() {
            for (service in services.values) {
                if (service is Startable) {
                    service.start()
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> getService(type: Class<T>): T {
            val service =
                services[type]
                    ?: throw IllegalArgumentException("Service not found: $type")
            return service as T
        }

        companion object {
            private var instance: ServiceManager? = null

            @JvmStatic
            fun getPreferencesService(): PreferencesService {
                return get().getService(PreferencesService::class.java)
            }

            @JvmStatic
            fun getCacheStorageService(): CacheStorageService {
                return get().getService(CacheStorageService::class.java)
            }

            @JvmStatic
            fun getPeriodicWorkService(): PeriodicWorkService {
                return get().getService(PeriodicWorkService::class.java)
            }

            @JvmStatic
            @JvmName("initialize") // This line can be deleted once OpenTelemetryRumBuilder migrates to Kotlin.
            internal fun initialize(appContext: Context?) {
                if (instance != null) {
                    return
                }
                instance =
                    ServiceManager(
                        listOf(
                            PreferencesService.create(appContext),
                            CacheStorageService(appContext),
                            PeriodicWorkService(),
                        ),
                    )
            }

            @JvmStatic
            @JvmName("get") // This line can be deleted once OpenTelemetryRumBuilder migrates to Kotlin.
            internal fun get(): ServiceManager {
                checkNotNull(instance) { "Services haven't been initialized" }
                return instance!!
            }
        }
    }
