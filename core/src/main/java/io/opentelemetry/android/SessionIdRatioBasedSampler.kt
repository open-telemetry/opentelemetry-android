/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingResult

/**
 * Session ID ratio based sampler. Uses [Sampler.traceIdRatioBased] sampler internally, but passes sessionId instead of
 * traceId to the underlying sampler in order to use the same ratio logic but on sessionId instead. This is valid as sessionId
 * uses [io.opentelemetry.api.trace.TraceId.fromLongs] internally to generate random session IDs.
 */
class SessionIdRatioBasedSampler(
    ratio: Double,
    private val sessionProvider: SessionProvider,
) : Sampler {
    // SessionId uses the same format as TraceId, so we can reuse trace ID ratio sampler.
    private val ratioBasedSampler: Sampler = Sampler.traceIdRatioBased(ratio)

    override fun shouldSample(
        parentContext: Context,
        traceId: String,
        name: String,
        spanKind: SpanKind,
        attributes: Attributes,
        parentLinks: List<LinkData?>,
    ): SamplingResult {
        // Replace traceId with sessionId
        return ratioBasedSampler.shouldSample(
            parentContext,
            sessionProvider.getSessionId(),
            name,
            spanKind,
            attributes,
            parentLinks,
        )
    }

    override fun getDescription(): String = "SessionIdRatioBased{traceIdRatioBased:$ratioBasedSampler.description}"
}
