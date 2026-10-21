/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.sdk.trace.samplers.Sampler

/** Configures sampling independently of session lifecycle settings. */
@OpenTelemetryDslMarker
class SamplerConfiguration internal constructor() {
    internal var traceSamplerFactory: ((SessionProvider) -> Sampler)? = null
        private set

    /**
     * Creates a trace sampler during initialization, using the final session provider if needed.
     * For example, `trace { Sampler.alwaysOn() }` or `trace { SessionIdRatioBasedSampler(0.5, it) }`.
     * The last factory supplied is used. It is not called when tracing is disabled, and a
     * factory failure aborts initialization. Logs and metrics are unaffected.
     * This setting preserves existing span session IDs, subject to SDK attribute limits.
     */
    @Incubating
    fun trace(factory: (SessionProvider) -> Sampler) {
        traceSamplerFactory = factory
    }
}
