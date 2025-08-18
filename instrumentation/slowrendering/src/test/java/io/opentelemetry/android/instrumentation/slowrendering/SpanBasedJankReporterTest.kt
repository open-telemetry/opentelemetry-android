package io.opentelemetry.android.instrumentation.slowrendering

import android.util.Log
import android.util.SparseIntArray
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import io.opentelemetry.sdk.trace.data.SpanData
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private val COUNT_KEY = AttributeKey.longKey("count")


class SpanBasedJankReporterTest {

    private lateinit var tracer: Tracer

    @Rule
    var otelTesting: OpenTelemetryRule = OpenTelemetryRule.create()

    @BeforeEach
    fun setup(){
        tracer = otelTesting.openTelemetry.getTracer("testTracer");
    }

    @Test
    fun `spans are generated`() {
        val jankReporter = SpanBasedJankReporter(tracer)
        val perActivityListener: PerActivityListener = mockk()
        val histogramData: SparseIntArray = mockk()
        every { histogramData.size() } returns 2
        val key1 = 17
        val key2 = 701
        every { histogramData.keyAt(0) } returns key1
        every { histogramData.keyAt(1) } returns key2
        every { histogramData.get(key1) } returns 3
        every { histogramData.get(key2) } returns 1
        every { perActivityListener.resetMetrics() } returns histogramData
        every { perActivityListener.getActivityName() } returns "io.otel/Komponent"
        mockkStatic(Log::class)
        every { Log.d(any(), any())} returns 0

        jankReporter.reportSlow(perActivityListener)

        assertSpanContent(otelTesting.spans)
    }

    private fun assertSpanContent(spans: MutableList<SpanData?>?) {
        assertThat<SpanData?>(spans)
            .hasSize(2)
            .satisfiesExactly(
                ThrowingConsumer { span: SpanData? ->
                    OpenTelemetryAssertions.assertThat(span)
                        .hasName("slowRenders")
                        .endsAt(span!!.getStartEpochNanos())
                        .hasAttribute(COUNT_KEY, 3L)
                        .hasAttribute(
                            AttributeKey.stringKey("activity.name"),
                            "io.otel/Komponent"
                        )
                },
                ThrowingConsumer { span: SpanData? ->
                    OpenTelemetryAssertions.assertThat(span)
                        .hasName("frozenRenders")
                        .endsAt(span!!.getStartEpochNanos())
                        .hasAttribute(COUNT_KEY, 1L)
                        .hasAttribute(
                            AttributeKey.stringKey("activity.name"),
                            "io.otel/Komponent"
                        )
                })
    }


}