/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.TraceState
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.IdGenerator
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SessionIdRatioBasedSamplerTest {
    @MockK
    lateinit var sessionProvider: SessionProvider
    private val traceId: String = idsGenerator.generateTraceId()
    private val parentContext: Context = Context.root().with(Span.getInvalid())
    private val parentLinks = mutableListOf<LinkData?>(LinkData.create(SpanContext.getInvalid()))

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun samplerDropsHigh() {
        every { sessionProvider.getSessionId() } returns HIGH_ID

        val sampler = SessionIdRatioBasedSampler(0.5, sessionProvider)

        // Sampler drops if TraceIdRatioBasedSampler would drop this sessionId
        assertThat(shouldSample(sampler)).isEqualTo(SamplingDecision.DROP)
    }

    @Test
    fun samplerKeepsLowestId() {
        // Sampler accepts if TraceIdRatioBasedSampler would accept this sessionId
        every { sessionProvider.getSessionId() } returns LOW_ID

        val sampler = SessionIdRatioBasedSampler(0.5, sessionProvider)
        assertThat(shouldSample(sampler)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE)
    }

    @Test
    fun zeroRatioDropsAll() {
        every { sessionProvider.getSessionId() } returns HIGH_ID

        val samplerHigh =
            SessionIdRatioBasedSampler(0.0, sessionProvider)
        assertThat(shouldSample(samplerHigh)).isEqualTo(SamplingDecision.DROP)

        every { sessionProvider.getSessionId() } returns LOW_ID

        val samplerLow =
            SessionIdRatioBasedSampler(0.0, sessionProvider)
        assertThat(shouldSample(samplerLow)).isEqualTo(SamplingDecision.DROP)
    }

    @Test
    fun oneRatioAcceptsAll() {
        every { sessionProvider.getSessionId() } returns HIGH_ID

        val samplerHigh =
            SessionIdRatioBasedSampler(1.0, sessionProvider)
        assertThat(shouldSample(samplerHigh)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE)

        every { sessionProvider.getSessionId() } returns LOW_ID

        val samplerLow =
            SessionIdRatioBasedSampler(1.0, sessionProvider)
        assertThat(shouldSample(samplerLow)).isEqualTo(SamplingDecision.RECORD_AND_SAMPLE)
    }

    @Test
    fun `sampling and exported attribution use one lookup`() {
        every { sessionProvider.getSessionId() } returnsMany listOf(LOW_ID, HIGH_ID)
        val exporter = InMemorySpanExporter.create()
        SdkTracerProvider
            .builder()
            .setSampler(SessionIdRatioBasedSampler(0.5, sessionProvider))
            .addSpanProcessor(SessionIdSpanAppender(sessionProvider, preserveExistingSessionId = true))
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build()
            .use { provider ->
                provider
                    .get("test")
                    .spanBuilder("sampled")
                    .setAttribute("session.id", HIGH_ID)
                    .startSpan()
                    .end()
                provider
                    .get("test")
                    .spanBuilder("dropped")
                    .startSpan()
                    .end()
                assertThat(exporter.finishedSpanItems).hasSize(1)
                assertThat(
                    exporter.finishedSpanItems
                        .single()
                        .attributes
                        .get(stringKey("session.id")),
                ).isEqualTo(LOW_ID)
            }
        verify(exactly = 2) { sessionProvider.getSessionId() }
    }

    @Test
    fun `sampling attributes preserve parent trace state`() {
        every { sessionProvider.getSessionId() } returns LOW_ID
        val traceState = TraceState.builder().put("vendor", "value").build()
        val result =
            SessionIdRatioBasedSampler(0.5, sessionProvider)
                .shouldSample(parentContext, traceId, "test", SpanKind.INTERNAL, Attributes.empty(), parentLinks)
        assertThat(result.attributes).isEqualTo(Attributes.of(stringKey("session.id"), LOW_ID))
        assertThat(result.getUpdatedTraceState(traceState)).isSameAs(traceState)
    }

    private fun shouldSample(sampler: Sampler): SamplingDecision? =
        sampler
            .shouldSample(
                parentContext,
                traceId,
                "name",
                SpanKind.INTERNAL,
                Attributes.empty(),
                parentLinks,
            ).decision

    companion object {
        private const val HIGH_ID = "00000000000000008fffffffffffffff"
        private const val LOW_ID = "00000000000000000000000000000000"
        private val idsGenerator: IdGenerator = IdGenerator.random()
    }
}
