/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.IOException
import java.util.concurrent.TimeUnit

class DefaultExportSchedulerTest {
    private lateinit var scheduler: DefaultExportScheduler

    @BeforeEach
    fun setUp() {
        scheduler = DefaultExportScheduler(mockk())
    }

    @AfterEach
    fun tearDown() {
        SignalFromDiskExporter.resetForTesting()
    }

    @Test
    fun `Try to export all available signals when running`() {
        val signalFromDiskExporter = mockk<SignalFromDiskExporter>()
        every { signalFromDiskExporter.exportBatchOfEach() }
            .returns(true)
            .andThen(true)
            .andThen(false)
        SignalFromDiskExporter.set(signalFromDiskExporter)

        scheduler.onRun()

        verify(exactly = 3) {
            signalFromDiskExporter.exportBatchOfEach()
        }
    }

    @Test
    fun `Avoid crashing when an exception happens during execution`() {
        val signalFromDiskExporter = mockk<SignalFromDiskExporter>()
        every { signalFromDiskExporter.exportBatchOfEach() }.throws(IOException())
        SignalFromDiskExporter.set(signalFromDiskExporter)

        try {
            scheduler.onRun()
        } catch (e: IOException) {
            fail(e)
        }
    }

    @Test
    fun `Stop running if it can't export from disk`() {
        assertThat(scheduler.shouldStopRunning()).isTrue()
    }

    @Test
    fun `Continue to run if it can export from disk`() {
        SignalFromDiskExporter.set(mockk())
        assertThat(scheduler.shouldStopRunning()).isFalse()
    }

    @Test
    fun `Verify minimum delay`() {
        assertThat(scheduler.minimumDelayUntilNextRunInMillis()).isEqualTo(
            TimeUnit.SECONDS.toMillis(
                10,
            ),
        )
    }
}
