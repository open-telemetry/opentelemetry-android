/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import androidx.test.ext.junit.runners.AndroidJUnit4
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

@RunWith(AndroidJUnit4::class)
class ServicesTest {
    @Test
    fun `Verify that services are created lazily and also closed`() {
        val factory = mockk<ServicesFactory>()
        val cacheStorage = mockk<CacheStorage>(relaxUnitFun = true)
        every { factory.createCacheStorage() }.returns(cacheStorage)
        val periodicWork = mockk<PeriodicWork>(relaxUnitFun = true)
        every { factory.createPeriodicWork() }.returns(periodicWork)
        val currentNetworkProvider = mockk<CurrentNetworkProvider>(relaxUnitFun = true)
        every { factory.createCurrentNetworkProvider() }.returns(currentNetworkProvider)
        val appLifecycle = mockk<AppLifecycle>(relaxUnitFun = true)
        every { factory.createAppLifecycle() }.returns(appLifecycle)
        val visibleScreenTracker = mockk<VisibleScreenTracker>(relaxUnitFun = true)
        every { factory.createVisibleScreenTracker() }.returns(visibleScreenTracker)

        // Instantiation of services must not create any service
        val services = Services(factory)
        verify { factory wasNot Called }

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

        // Verify closing services
        services.close()
        verify {
            cacheStorage.close()
            periodicWork.close()
            currentNetworkProvider.close()
            appLifecycle.close()
            visibleScreenTracker.close()
        }
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
