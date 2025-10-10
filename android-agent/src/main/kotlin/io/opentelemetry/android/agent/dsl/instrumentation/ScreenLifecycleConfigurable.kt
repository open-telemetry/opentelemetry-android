/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.api.trace.Tracer

internal interface ScreenLifecycleConfigurable {
    fun tracerCustomizer(value: (Tracer) -> Tracer)

    fun screenNameExtractor(value: ScreenNameExtractor)
}
