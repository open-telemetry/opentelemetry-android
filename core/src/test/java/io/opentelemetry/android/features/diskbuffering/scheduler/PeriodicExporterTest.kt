/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PeriodicExporterTest {
    @Test
    fun `Use default export period`() {
        val exporter = PeriodicExporter(mockk())

        assertThat(exporter.period()).isEqualTo(10.seconds)
    }

    @Test
    fun `Use configured export period`() {
        val exporter = PeriodicExporter(mockk(), 250.milliseconds)

        assertThat(exporter.period()).isEqualTo(250.milliseconds)
    }

    @Test
    fun `Export all signals when run`() {
        val delegate = mockk<SignalFromDiskExporter>()
        every { delegate.exportAllSignalsFromDisk() } returns true
        val exporter = PeriodicExporter(delegate)

        exporter.run()

        verify(exactly = 1) { delegate.exportAllSignalsFromDisk() }
    }

    @Test
    fun `Stop future executions when stopped`() {
        val exporter = PeriodicExporter(mockk())

        exporter.stop()

        assertThat(exporter.shouldStop()).isTrue()
    }
}
