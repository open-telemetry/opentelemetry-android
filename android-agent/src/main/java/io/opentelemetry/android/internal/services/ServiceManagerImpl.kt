/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleService
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService
import java.util.Collections

internal class ServiceManagerImpl(services: List<Any>) : ServiceManager {
    private val services: Map<Class<out Any>, Any>

    init {
        val map: MutableMap<Class<out Any>, Any> = HashMap()
        for (service in services) {
            map[service.javaClass] = service
        }
        this.services = Collections.unmodifiableMap(map)
    }

    override fun getPreferences(): Preferences {
        return getService(Preferences::class.java)
    }

    override fun getCacheStorage(): CacheStorage {
        return getService(CacheStorage::class.java)
    }

    override fun getPeriodicWorkService(): PeriodicWorkService {
        return getService(PeriodicWorkService::class.java)
    }

    override fun getCurrentNetworkProvider(): CurrentNetworkProvider {
        return getService(CurrentNetworkProvider::class.java)
    }

    override fun getAppLifecycleService(): AppLifecycleService {
        return getService(AppLifecycleService::class.java)
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
}
