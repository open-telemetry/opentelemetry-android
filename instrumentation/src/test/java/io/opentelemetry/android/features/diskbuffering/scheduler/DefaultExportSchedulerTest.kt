/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.features.diskbuffering.SignalDiskExporter
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
        scheduler = DefaultExportScheduler()
    }

    @AfterEach
    fun tearDown() {
        SignalDiskExporter.resetForTesting()
    }

    @Test
    fun `Try to export all available signals when running`() {
        val signalDiskExporter = mockk<SignalDiskExporter>()
        every { signalDiskExporter.exportBatchOfEach() }.returns(true).andThen(true).andThen(false)
        SignalDiskExporter.set(signalDiskExporter)

        scheduler.onRun()

        verify(exactly = 3) {
            signalDiskExporter.exportBatchOfEach()
        }
    }

    @Test
    fun `Avoid crashing when an exception happens during execution`() {
        val signalDiskExporter = mockk<SignalDiskExporter>()
        every { signalDiskExporter.exportBatchOfEach() }.throws(IOException())
        SignalDiskExporter.set(signalDiskExporter)

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
        SignalDiskExporter.set(mockk())
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
