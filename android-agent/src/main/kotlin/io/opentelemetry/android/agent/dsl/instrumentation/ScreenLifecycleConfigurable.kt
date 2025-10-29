/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.api.trace.Tracer

internal interface ScreenLifecycleConfigurable {
    /**
     * Sets a function that can be used to customize the trace.
     */
    fun tracerCustomizer(value: (Tracer) -> Tracer)

    /**
     * Defines how screen names should be extracted.
     */
    fun screenNameExtractor(value: ScreenNameExtractor)
}
