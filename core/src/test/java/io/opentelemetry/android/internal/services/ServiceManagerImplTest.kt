/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import io.opentelemetry.android.internal.services.ServiceManager.Companion.initialize
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ServiceManagerImplTest {
    @Test
    fun verifyAvailableServices() {
        initialize(RuntimeEnvironment.getApplication())

        val serviceManager = ServiceManager.get()

        assertThat(serviceManager.getPeriodicWorkService()).isInstanceOf(PeriodicWorkService::class.java)
        assertThat(serviceManager.getCacheStorage()).isInstanceOf(CacheStorage::class.java)
        assertThat(serviceManager.getPreferences()).isInstanceOf(Preferences::class.java)
        assertThat(serviceManager.getCurrentNetworkProvider()).isInstanceOf(CurrentNetworkProvider::class.java)
    }

    @Test
    fun delegatingStartCall() {
        val firstService = Mockito.mock<FirstService>()
        val secondService = Mockito.mock<SecondService>() // SecondService is not "Startable".
        val serviceManager = ServiceManagerImpl(listOf(firstService, secondService))

        serviceManager.start()

        verify(firstService).start()
        verifyNoMoreInteractions(firstService)
        verifyNoInteractions(secondService)
    }

    private class FirstService : Startable {
        override fun start() {
        }
    }

    private class SecondService
}
