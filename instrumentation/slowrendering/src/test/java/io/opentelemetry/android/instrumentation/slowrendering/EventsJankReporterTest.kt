/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.util.Log
import android.util.SparseIntArray
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.jupiter.api.Test

class EventsJankReporterTest {
    @Rule
    var otelTesting: OpenTelemetryRule = OpenTelemetryRule.create()

    @Test
    fun `event is generated`() {
        val eventLogger = otelTesting.openTelemetry.logsBridge.get("JANK!")
        val jankReporter = EventsJankReporter(eventLogger, 0.600)
        val histogramData: SparseIntArray = mockk()
        every { histogramData.size() } returns 2
        val key1 = 17
        val key2 = 701
        every { histogramData.keyAt(0) } returns key1
        every { histogramData.keyAt(1) } returns key2
        every { histogramData.get(key1) } returns 3
        every { histogramData.get(key2) } returns 1
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        jankReporter.reportSlow(histogramData, 10.5, "io.otel/Komponent")

        assertThat(otelTesting.logRecords.size).isEqualTo(1)
        val log = otelTesting.logRecords.get(0)
        assertThat(log.eventName).isEqualTo("app.jank")
        assertThat(log.attributes.get(FRAME_COUNT)).isEqualTo(1)
        assertThat(log.attributes.get(PERIOD)).isEqualTo(10.5)
        assertThat(log.attributes.get(THRESHOLD)).isEqualTo(0.6)
    }
}
