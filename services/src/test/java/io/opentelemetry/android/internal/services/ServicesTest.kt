/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.internal.services.Services.ServicesFactory
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWork
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ServicesTest {
    @Test
    fun `Verify that services are created lazily`() {
        val factory = mockk<ServicesFactory>()
        val preferences = mockk<Preferences>()
        every { factory.createPreferences() }.returns(preferences)
        val cacheStorage = mockk<CacheStorage>()
        every { factory.createCacheStorage() }.returns(cacheStorage)
        val periodicWork = mockk<PeriodicWork>()
        every { factory.createPeriodicWork() }.returns(periodicWork)
        val currentNetworkProvider = mockk<CurrentNetworkProvider>()
        every { factory.createCurrentNetworkProvider() }.returns(currentNetworkProvider)
        val appLifecycle = mockk<AppLifecycle>()
        every { factory.createAppLifecycle() }.returns(appLifecycle)
        val visibleScreenTracker = mockk<VisibleScreenTracker>()
        every { factory.createVisibleScreenTracker() }.returns(visibleScreenTracker)

        // Instantiation of services must not create any service
        val services = Services(factory)
        verify { factory wasNot Called }

        verifyLazyCreation(preferences, services::preferences, factory::createPreferences)
        verifyLazyCreation(cacheStorage, services::cacheStorage, factory::createCacheStorage)
        verifyLazyCreation(periodicWork, services::periodicWork, factory::createPeriodicWork)
        verifyLazyCreation(
            currentNetworkProvider,
            services::currentNetworkProvider,
            factory::createCurrentNetworkProvider,
        )
        verifyLazyCreation(appLifecycle, services::appLifecycle, factory::createAppLifecycle)
        verifyLazyCreation(
            visibleScreenTracker,
            services::visibleScreenTracker,
            factory::createVisibleScreenTracker,
        )
    }

    private fun <T : Any> verifyLazyCreation(
        expectedObject: T,
        serviceGetter: () -> T,
        factoryMethodCall: () -> Unit,
    ) {
        // Initial state
        verify(exactly = 0) { factoryMethodCall.invoke() }

        // Calling each getter twice must trigger creation once
        assertThat(serviceGetter()).isEqualTo(expectedObject)
        assertThat(serviceGetter()).isEqualTo(expectedObject)
        verify(exactly = 1) { factoryMethodCall.invoke() }
    }
}
