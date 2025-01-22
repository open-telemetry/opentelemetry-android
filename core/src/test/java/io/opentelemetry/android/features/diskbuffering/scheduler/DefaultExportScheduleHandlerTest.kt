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
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWork
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultExportScheduleHandlerTest {
    private lateinit var handler: DefaultExportScheduleHandler
    private lateinit var periodicWork: PeriodicWork

    @BeforeEach
    fun setUp() {
        periodicWork = createPeriodicWorkServiceMock()
        handler =
            DefaultExportScheduleHandler(
                DefaultExportScheduler { periodicWork },
            ) { periodicWork }
    }

    @Test
    fun `Start scheduler once when enabled`() {
        val captor = slot<Runnable>()

        // Calling enable the first time (should work)
        handler.enable()
        verify {
            periodicWork.enqueue(capture(captor))
        }
        assertThat(captor.captured).isInstanceOf(DefaultExportScheduler::class.java)
        clearAllMocks()

        // Calling enable a second time (should not work)
        handler.enable()
        verify(exactly = 0) {
            periodicWork.enqueue(any())
        }
    }

    private fun createPeriodicWorkServiceMock(): PeriodicWork {
        val periodicWork = mockk<PeriodicWork>()
        every { periodicWork.enqueue(any()) } just Runs
        return periodicWork
    }
}
