/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionIdRatioBasedSamplerTest {
    private static final String HIGH_ID = "00000000000000008fffffffffffffff";
    private static final Supplier<String> HIGH_ID_SUPPLIER = () -> HIGH_ID;
    private static final String LOW_ID = "00000000000000000000000000000000";
    private static final Supplier<String> LOW_ID_SAMPLER = () -> LOW_ID;
    private static final IdGenerator idsGenerator = IdGenerator.random();

    private final String traceId = idsGenerator.generateTraceId();
    private final Context parentContext = Context.root().with(Span.getInvalid());
    private final List<LinkData> parentLinks =
            Collections.singletonList(LinkData.create(SpanContext.getInvalid()));

    @Test
    void samplerDropsHigh() {
        SessionIdRatioBasedSampler sampler = new SessionIdRatioBasedSampler(0.5, HIGH_ID_SUPPLIER);

        // Sampler drops if TraceIdRatioBasedSampler would drop this sessionId
        assertEquals(shouldSample(sampler), SamplingDecision.DROP);
    }

    @Test
    void samplerKeepsLowestId() {
        // Sampler accepts if TraceIdRatioBasedSampler would accept this sessionId
        SessionIdRatioBasedSampler sampler = new SessionIdRatioBasedSampler(0.5, LOW_ID_SAMPLER);
        assertEquals(shouldSample(sampler), SamplingDecision.RECORD_AND_SAMPLE);
    }

    @Test
    void zeroRatioDropsAll() {
        SessionIdRatioBasedSampler samplerHigh =
                new SessionIdRatioBasedSampler(0.0, HIGH_ID_SUPPLIER);
        assertEquals(shouldSample(samplerHigh), SamplingDecision.DROP);
        SessionIdRatioBasedSampler samplerLow = new SessionIdRatioBasedSampler(0.0, LOW_ID_SAMPLER);
        assertEquals(shouldSample(samplerLow), SamplingDecision.DROP);
    }

    @Test
    void oneRatioAcceptsAll() {
        SessionIdRatioBasedSampler samplerHigh =
                new SessionIdRatioBasedSampler(1.0, HIGH_ID_SUPPLIER);
        assertEquals(shouldSample(samplerHigh), SamplingDecision.RECORD_AND_SAMPLE);
        SessionIdRatioBasedSampler samplerLow = new SessionIdRatioBasedSampler(1.0, LOW_ID_SAMPLER);
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
