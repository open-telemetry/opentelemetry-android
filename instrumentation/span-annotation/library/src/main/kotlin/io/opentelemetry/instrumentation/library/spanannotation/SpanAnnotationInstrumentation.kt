/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.spanannotation

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.trace.Tracer

@AutoService(AndroidInstrumentation::class)
class SpanAnnotationInstrumentation : AndroidInstrumentation {
    override val name: String = "span-annotation"

    override fun install(ctx: InstallationContext) {
        tracer =
            ctx.openTelemetry
                .tracerProvider
                .tracerBuilder("io.opentelemetry.android.instrumentation.span-annotation")
                .build()
    }

    companion object {
        lateinit var tracer: Tracer
    }
}
