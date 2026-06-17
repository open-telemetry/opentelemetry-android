/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_JANK_FRAME_COUNT
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_JANK_PERIOD
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_JANK_THRESHOLD
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.jupiter.api.Test

class EventJankReporterTest {
    @Rule
    var otelTesting: OpenTelemetryRule = OpenTelemetryRule.create()

    @OptIn(IncubatingApi::class)
    @Test
    fun `event is generated`() {
        val eventLogger = otelTesting.openTelemetry.logsBridge.get("JANK!")
        val jankReporter = EventJankReporter(eventLogger, 0.600)
        val histogramData = HashMap<Int, Int>()
        histogramData[17] = 3
        histogramData[701] = 1

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        jankReporter.reportSlow(histogramData, 10.5, "io.otel/Komponent")

        assertThat(otelTesting.logRecords.size).isEqualTo(1)
        val log = otelTesting.logRecords.get(0)
        assertThat(log.eventName).isEqualTo("app.jank")
        assertThat(log.attributes.get(longKey(APP_JANK_FRAME_COUNT))).isEqualTo(1)
        assertThat(log.attributes.get(doubleKey(APP_JANK_PERIOD))).isEqualTo(10.5)
        assertThat(log.attributes.get(doubleKey(APP_JANK_THRESHOLD))).isEqualTo(0.6)
    }
}
