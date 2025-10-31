package io.opentelemetry.instrumentation.agent.spanannotation.advice.constructor

import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.library.spanannotation.HelperFunctions
import net.bytebuddy.asm.Advice
import java.lang.reflect.Constructor

object SpanAttributeConstructorAdvice {

    @JvmStatic
    @Advice.OnMethodEnter(suppress = Throwable::class)
    fun onEnter(
        @Advice.AllArguments args: Array<Any?>,
        @Advice.Origin constructor: Constructor<*>
    ) {
        HelperFunctions.argAsAttribute(
            Span.current(),
            constructor.parameterAnnotations,
            args,
            constructor.declaringClass.simpleName
        )
    }
}