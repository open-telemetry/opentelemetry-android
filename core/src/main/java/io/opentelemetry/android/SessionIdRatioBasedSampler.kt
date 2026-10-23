/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.internal.processors.SessionIdRatioBasedPredicate
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import io.opentelemetry.sdk.trace.samplers.SamplingResult
import java.util.function.Predicate

/**
 * Session ID ratio based sampler. Uses [Sampler.traceIdRatioBased] sampler internally, but passes sessionId instead of
 * traceId to the underlying sampler in order to use the same ratio logic but on sessionId instead. This is valid as sessionId
 * uses [io.opentelemetry.api.trace.TraceId.fromLongs] internally to generate random session IDs.
 */
@OptIn(IncubatingApi::class)
class SessionIdRatioBasedSampler internal constructor(
    private val sessionPredicate: Predicate<String>,
    private val sessionProvider: SessionProvider,
) : Sampler {
    constructor(ratio: Double, sessionProvider: SessionProvider) : this(SessionIdRatioBasedPredicate(ratio), sessionProvider)

    override fun shouldSample(
        parentContext: Context,
        traceId: String,
        name: String,
        spanKind: SpanKind,
        attributes: Attributes,
        parentLinks: List<LinkData?>,
    ): SamplingResult {
        val sessionId = sessionProvider.getSessionId()
        return SamplingResult.create(
            if (sessionPredicate.test(sessionId)) SamplingDecision.RECORD_AND_SAMPLE else SamplingDecision.DROP,
            Attributes.of(SESSION_ID, sessionId),
        )
    }

    override fun getDescription(): String = "SessionIdRatioBased{traceIdRatioBased:$sessionPredicate.description}"

    private companion object {
        val SESSION_ID = stringKey(io.opentelemetry.kotlin.semconv.SessionAttributes.SESSION_ID)
    }
}
