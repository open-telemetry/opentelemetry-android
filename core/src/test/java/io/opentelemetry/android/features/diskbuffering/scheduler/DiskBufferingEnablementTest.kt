/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.internal.services.periodic.PeriodicRunnable
import io.opentelemetry.android.internal.services.periodic.PeriodicTaskScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DiskBufferingEnablementTest {
    private lateinit var state: DiskBufferingEnablement
    private lateinit var taskScheduler: PeriodicTaskScheduler

    @BeforeEach
    fun setUp() {
        taskScheduler = mockk()
        every { taskScheduler.start(any()) } just Runs
        state = DiskBufferingEnablement(mockk<SignalFromDiskExporter>(), taskScheduler)
    }

    @Test
    fun `Start scheduler once when enabled`() {
        val captor = slot<PeriodicRunnable>()

        state.enable()
        verify { taskScheduler.start(capture(captor)) }
        assertThat(captor.captured).isInstanceOf(PeriodicExporter::class.java)

        state.enable()
        verify(exactly = 1) { taskScheduler.start(any()) }
    }

    @Test
    fun `Stop scheduled work when disabled`() {
        val captor = slot<PeriodicRunnable>()

        state.enable()
        verify { taskScheduler.start(capture(captor)) }

        state.disable()

        assertThat(captor.captured.shouldStop()).isTrue()
    }

    @Test
    fun `Restart scheduled work when re-enabled`() {
        val captured = mutableListOf<PeriodicRunnable>()

        state.enable()
        state.disable()
        state.enable()

        verify(exactly = 2) { taskScheduler.start(capture(captured)) }

        assertThat(captured).hasSize(2)
        assertThat(captured[0].shouldStop()).isTrue()
        assertThat(captured[1].shouldStop()).isFalse()
        assertThat(captured[1]).isNotSameAs(captured[0])
    }
}
