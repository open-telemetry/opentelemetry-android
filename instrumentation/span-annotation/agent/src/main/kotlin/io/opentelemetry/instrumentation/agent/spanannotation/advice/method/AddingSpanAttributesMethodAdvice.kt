/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.spanannotation.advice.method

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.library.spanannotation.HelperFunctions
import net.bytebuddy.asm.Advice
import java.lang.reflect.Method

object AddingSpanAttributesMethodAdvice {
    @JvmStatic
    @Advice.OnMethodEnter(suppress = Throwable::class)
    fun onEnter(
        @Advice.AllArguments args: Array<Any?>,
        @Advice.Origin("#m") methodName: String,
    ) {
        HelperFunctions.argsAsAttributes(
            Span.current(),
            args,
            methodName,
        )
    }
}
