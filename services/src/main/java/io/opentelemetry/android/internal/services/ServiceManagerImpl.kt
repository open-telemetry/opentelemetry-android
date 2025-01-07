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
import java.util.Collections

internal class ServiceManagerImpl(
    services: List<Any>,
) : ServiceManager {
    private val services: Map<Class<out Any>, Any>

    init {
        val map: MutableMap<Class<out Any>, Any> = HashMap()
        for (service in services) {
            map[service.javaClass] = service
        }
        this.services = Collections.unmodifiableMap(map)
    }

    companion object {
        @JvmStatic
        fun create(application: Application): ServiceManager =
            ServiceManagerImpl(
                listOf(
                    Preferences.create(application),
                    CacheStorage(application),
                    PeriodicWorkService(),
                    CurrentNetworkProvider.create(application),
                    AppLifecycleService.create(),
                    VisibleScreenService.create(application),
                ),
            )
    }

    override fun getPreferences(): Preferences = getService(Preferences::class.java)

    override fun getCacheStorage(): CacheStorage = getService(CacheStorage::class.java)

    override fun getPeriodicWorkService(): PeriodicWorkService = getService(PeriodicWorkService::class.java)

    override fun getCurrentNetworkProvider(): CurrentNetworkProvider = getService(CurrentNetworkProvider::class.java)

    override fun getAppLifecycleService(): AppLifecycleService = getService(AppLifecycleService::class.java)

    override fun getVisibleScreenService(): VisibleScreenService = getService(VisibleScreenService::class.java)

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
