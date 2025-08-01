/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.IdGenerator
import io.opentelemetry.sdk.trace.data.LinkData
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
