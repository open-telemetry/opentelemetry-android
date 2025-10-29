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
        @Advice.Origin method: Method
    ) {
        HelperFunctions.argsAsAttributes(
            Span.current(),
            args,
            method.name
        )
    }
}