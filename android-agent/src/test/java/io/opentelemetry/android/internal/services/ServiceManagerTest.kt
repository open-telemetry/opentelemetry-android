/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services

import io.opentelemetry.android.internal.services.ServiceManager.Companion.initialize
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
class ServiceManagerTest {
    @Test
    fun verifyAvailableServices() {
        initialize(RuntimeEnvironment.getApplication())

        assertThat(ServiceManager.getPeriodicWorkService()).isInstanceOf(PeriodicWorkService::class.java)
        assertThat(ServiceManager.getCacheStorageService()).isInstanceOf(CacheStorageService::class.java)
        assertThat(ServiceManager.getPreferencesService()).isInstanceOf(PreferencesService::class.java)
    }

    @Test
    fun delegatingStartCall() {
        val firstService = Mockito.mock<FirstService>()
        val secondService = Mockito.mock<SecondService>()
        val serviceManager = ServiceManager(listOf(firstService, secondService))

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
