/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import io.opentelemetry.android.sampling.SessionIdRatioBasedSampler;
import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionIdRatioBasedSamplerTest {
    private static final String HIGH_ID = "00000000000000008fffffffffffffff";
    private static final String LOW_ID = "00000000000000000000000000000000";
    private static final IdGenerator idsGenerator = IdGenerator.random();

    @Mock SessionProvider sessionProvider;
    private final String traceId = idsGenerator.generateTraceId();
    private final Context parentContext = Context.root().with(Span.getInvalid());
    private final List<LinkData> parentLinks =
            Collections.singletonList(LinkData.create(SpanContext.getInvalid()));

    @Test
    void samplerDropsHigh() {
        when(sessionProvider.getSessionId()).thenReturn(HIGH_ID);

        SessionIdRatioBasedSampler sampler = new SessionIdRatioBasedSampler(0.5, sessionProvider);

        // Sampler drops if TraceIdRatioBasedSampler would drop this sessionId
        assertEquals(shouldSample(sampler), SamplingDecision.DROP);
    }

    @Test
    void samplerKeepsLowestId() {
        // Sampler accepts if TraceIdRatioBasedSampler would accept this sessionId
        when(sessionProvider.getSessionId()).thenReturn(LOW_ID);

        SessionIdRatioBasedSampler sampler = new SessionIdRatioBasedSampler(0.5, sessionProvider);
        assertEquals(shouldSample(sampler), SamplingDecision.RECORD_AND_SAMPLE);
    }

    @Test
    void zeroRatioDropsAll() {
        when(sessionProvider.getSessionId()).thenReturn(HIGH_ID);

        SessionIdRatioBasedSampler samplerHigh =
                new SessionIdRatioBasedSampler(0.0, sessionProvider);
        assertEquals(shouldSample(samplerHigh), SamplingDecision.DROP);

        when(sessionProvider.getSessionId()).thenReturn(LOW_ID);

        SessionIdRatioBasedSampler samplerLow =
                new SessionIdRatioBasedSampler(0.0, sessionProvider);
        assertEquals(shouldSample(samplerLow), SamplingDecision.DROP);
    }

    @Test
    void oneRatioAcceptsAll() {
        when(sessionProvider.getSessionId()).thenReturn(HIGH_ID);

        SessionIdRatioBasedSampler samplerHigh =
                new SessionIdRatioBasedSampler(1.0, sessionProvider);
        assertEquals(shouldSample(samplerHigh), SamplingDecision.RECORD_AND_SAMPLE);

        when(sessionProvider.getSessionId()).thenReturn(LOW_ID);

        SessionIdRatioBasedSampler samplerLow =
                new SessionIdRatioBasedSampler(1.0, sessionProvider);
        assertEquals(shouldSample(samplerLow), SamplingDecision.RECORD_AND_SAMPLE);
    }

    private SamplingDecision shouldSample(Sampler sampler) {
        return sampler.shouldSample(
                        parentContext,
                        traceId,
                        "name",
                        SpanKind.INTERNAL,
                        Attributes.empty(),
                        parentLinks)
                .getDecision();
    }
}
