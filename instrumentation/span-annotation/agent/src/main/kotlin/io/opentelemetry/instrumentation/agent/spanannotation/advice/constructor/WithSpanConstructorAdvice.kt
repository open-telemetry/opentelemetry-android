package io.opentelemetry.instrumentation.agent.spanannotation.advice.constructor

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope
import io.opentelemetry.instrumentation.library.spanannotation.HelperFunctions
import io.opentelemetry.instrumentation.annotations.WithSpan
import net.bytebuddy.asm.Advice
import java.lang.reflect.Constructor

object WithSpanConstructorAdvice {

    @JvmStatic
    @Advice.OnMethodEnter(suppress = Throwable::class)
    fun onEnter(
        @Advice.Origin constructor: Constructor<*>
    ) : Pair<Span, Scope> {
        val withSpan = constructor.getAnnotation(WithSpan::class.java)
            ?: throw IllegalStateException("WithSpan annotation not found on constructor ${constructor.declaringClass.simpleName}")

        return HelperFunctions.startSpan(withSpan, constructor.declaringClass.simpleName)
    }

    @JvmStatic
    @Advice.OnMethodExit(suppress = Throwable::class)
    fun onExit(
        @Advice.Enter spanPair: Pair<Span, Scope>
    ) {
        HelperFunctions.stopSpan(spanPair, null)
    }
}