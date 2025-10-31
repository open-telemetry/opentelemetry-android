package io.opentelemetry.instrumentation.library.spanannotation

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Scope
import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlin.text.ifEmpty

object HelperFunctions {

    @JvmStatic
    fun startSpan(withSpan: WithSpan, name: String): Pair<Span, Scope> {
        val spanBuilder = SpanAnnotationInstrumentation
            .tracer
            .spanBuilder(withSpan.value.ifEmpty { name })
            .setSpanKind(withSpan.kind)

        if (!withSpan.inheritContext) {
            spanBuilder.setNoParent()
        }

        val span = spanBuilder.startSpan()
        val scope = span.makeCurrent()

        return Pair(span, scope)
    }

    @JvmStatic
    fun stopSpan(spanPair: Pair<Span, Scope>, throwable: Throwable?) {
        spanPair.let { (span, scope) ->
            throwable?.let {
                span.recordException(throwable)
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, it.message ?: "Exception thrown")
            }
            scope.close()
            span.end()
        }
    }

    @JvmStatic
    fun argAsAttribute(span: Span, parameterAnnotations:  Array<Array<Annotation>>, args: Array<Any?>, name: String) {
        args.forEachIndexed { index, arg ->
            parameterAnnotations[index]
                .filterIsInstance<SpanAttribute>()
                .firstOrNull()?.let { spanAttribute ->
                    val attributeKey = spanAttribute.value.takeIf { it.isNotEmpty() } ?: "arg${index}_$name"
                    span.setAttribute(attributeKey, arg.toString())
                }
        }
    }

    @JvmStatic
    fun argsAsAttributes(span: Span, args: Array<Any?>, name: String) {
        args.forEachIndexed { index, arg ->
            val attributeKey = "arg${index}_$name"
            span.setAttribute(attributeKey, arg.toString())
        }
    }
}