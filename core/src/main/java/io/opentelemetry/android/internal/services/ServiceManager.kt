/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

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
}
