/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import java.util.List;

/**
 * Session ID ratio based sampler. Uses {@link Sampler#traceIdRatioBased(double)} sampler
 * internally, but passes sessionId instead of traceId to the underlying sampler in order to use the
 * same ratio logic but on sessionId instead. This is valid as sessionId uses {@link
 * io.opentelemetry.api.trace.TraceId#fromLongs(long, long)} internally to generate random session
 * IDs.
 */
public class SessionIdRatioBasedSampler implements Sampler {
    private final Sampler ratioBasedSampler;
    private final SessionProvider sessionProvider;

    public SessionIdRatioBasedSampler(double ratio, SessionProvider sessionProvider) {
        this.sessionProvider = sessionProvider;
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
                parentContext, sessionProvider.getSessionId(), name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return String.format(
                "SessionIdRatioBased{traceIdRatioBased:%s}",
                this.ratioBasedSampler.getDescription());
    }
}
