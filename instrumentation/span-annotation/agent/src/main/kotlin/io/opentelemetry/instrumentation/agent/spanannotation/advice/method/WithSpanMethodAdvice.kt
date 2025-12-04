/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.spanannotation.advice.method

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope
import io.opentelemetry.instrumentation.annotations.WithSpan
import io.opentelemetry.instrumentation.library.spanannotation.HelperFunctions
import net.bytebuddy.asm.Advice
import java.lang.reflect.Method

object WithSpanMethodAdvice {
    @JvmStatic
    @Advice.OnMethodEnter(suppress = Throwable::class)
    fun onEnter(
        @Advice.Origin method: Method,
    ): Pair<Span, Scope> {
        val withSpan =
            method.getAnnotation(WithSpan::class.java)
                ?: error("WithSpan annotation not found on method ${method.name}")

        return HelperFunctions.startSpan(
            withSpan,
            method.name,
        )
    }

    @JvmStatic
    @Advice.OnMethodExit(suppress = Throwable::class, onThrowable = Throwable::class)
    fun onExit(
        @Advice.Enter spanPair: Pair<Span, Scope>,
        @Advice.Thrown throwable: Throwable?,
    ) {
        HelperFunctions.stopSpan(
            spanPair,
            throwable,
        )
    }
}
