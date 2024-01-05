/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWorkService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultExportScheduleHandlerTest {
    private lateinit var handler: DefaultExportScheduleHandler

    @BeforeEach
    fun setUp() {
        handler = DefaultExportScheduleHandler()
    }

    @Test
    fun `Start scheduler once when enabled`() {
        val periodicWorkService = createMock()
        val captor = slot<Runnable>()

        // Calling enable the first time (should work)
        handler.enable()
        verify {
            periodicWorkService.enqueue(capture(captor))
        }
        assertThat(captor.captured).isInstanceOf(DefaultExportScheduler::class.java)
        clearAllMocks()

        // Calling enable a second time (should not work)
        handler.enable()
        verify(exactly = 0) {
            periodicWorkService.enqueue(any())
        }
    }

    private fun createMock(): PeriodicWorkService {
        val periodicWorkService = mockk<PeriodicWorkService>()
        val manager = mockk<ServiceManager>()
        every {
            manager.getService(PeriodicWorkService::class.java)
        }.returns(periodicWorkService)
        every { periodicWorkService.enqueue(any()) } just Runs
        ServiceManager.setForTest(manager)

        return periodicWorkService
    }
}
