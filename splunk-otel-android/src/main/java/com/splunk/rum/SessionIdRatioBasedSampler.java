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

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;
import java.util.function.Supplier;

/**
 * Session ID ratio based sampler. Uses {@link Sampler#traceIdRatioBased(double)} sampler
 * internally, but passes sessionId instead of traceId to the underlying sampler in order to use the
 * same ratio logic but on sessionId instead. This is valid as sessionId uses {@link
 * io.opentelemetry.api.trace.TraceId#fromLongs(long, long)} internally to generate random session
 * IDs.
 */
class SessionIdRatioBasedSampler implements Sampler {
    private final Sampler ratioBasedSampler;
    private final Supplier<SplunkRum> splunkRumSupplier;

    SessionIdRatioBasedSampler(double ratio, Supplier<SplunkRum> splunkRumSupplier) {
        this.splunkRumSupplier = splunkRumSupplier;
        // SessionId uses the same format as TraceId, so we can reuse trace ID ratio sampler.
        this.ratioBasedSampler = Sampler.traceIdRatioBased(ratio);
    }

    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {
        // Replace traceId with sessionId
        return ratioBasedSampler.shouldSample(
                parentContext,
                splunkRumSupplier.get().getRumSessionId(),
                name,
                spanKind,
                attributes,
                parentLinks);
    }

    @Override
    public String getDescription() {
        return String.format(
                "SessionIdRatioBased{traceIdRatioBased:%s}",
                this.ratioBasedSampler.getDescription());
    }
}
