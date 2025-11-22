/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.jupiter.api.Test

class EventJankReporterTest {
    @Rule
    var otelTesting: OpenTelemetryRule = OpenTelemetryRule.create()

    @Test
    fun `event is generated`() {
        val eventLogger = otelTesting.openTelemetry.logsBridge.get("JANK!")
        val jankReporter = EventJankReporter(eventLogger, SessionProvider.getNoop(), 0.600)
        val histogramData = HashMap<Int, Int>()
        histogramData[17] = 3
        histogramData[701] = 1

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
