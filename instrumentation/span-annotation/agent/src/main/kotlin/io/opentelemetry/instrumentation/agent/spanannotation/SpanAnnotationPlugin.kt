package io.opentelemetry.instrumentation.agent.spanannotation

import io.opentelemetry.instrumentation.agent.spanannotation.advice.constructor.AddingSpanAttributesConstructorAdvice
import io.opentelemetry.instrumentation.agent.spanannotation.advice.constructor.SpanAttributeConstructorAdvice
import io.opentelemetry.instrumentation.agent.spanannotation.advice.constructor.WithSpanConstructorAdvice
import io.opentelemetry.instrumentation.agent.spanannotation.advice.method.AddingSpanAttributesMethodAdvice
import io.opentelemetry.instrumentation.agent.spanannotation.advice.method.SpanAttributeMethodAdvice
import io.opentelemetry.instrumentation.agent.spanannotation.advice.method.WithSpanMethodAdvice
import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers

const val WITH_SPAN_ANNOTATION = "io.opentelemetry.instrumentation.annotations.WithSpan"
const val SPAN_ATTRIBUTE_ANNOTATION = "io.opentelemetry.instrumentation.annotations.SpanAttribute"
const val ADDING_SPAN_ATTRIBUTES_ANNOTATION = "io.opentelemetry.instrumentation.annotations.AddingSpanAttributes"

class SpanAnnotationPlugin : Plugin {
    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator
    ): DynamicType.Builder<*> {
        return builder
            // Apply advice to methods annotated with @WithSpan
            .visit(
                Advice.to(WithSpanMethodAdvice::class.java)
                    .on(
                        ElementMatchers.not(ElementMatchers.isConstructor())
                            .and(ElementMatchers.isAnnotatedWith(
                                ElementMatchers.named(WITH_SPAN_ANNOTATION)
                            )
                        )
                    )
            )
            // Apply advice to constructors annotated with @WithSpan
            .visit(
                Advice.to(WithSpanConstructorAdvice::class.java)
                    .on(
                        ElementMatchers.isConstructor<MethodDescription>()
                            .and(ElementMatchers.isAnnotatedWith(
                                ElementMatchers.named(WITH_SPAN_ANNOTATION)
                            )
                        )
                    )
            )
            // Apply advice to methods annotated with @AddingSpanAttributes
            .visit(
                Advice.to(AddingSpanAttributesMethodAdvice::class.java)
                    .on(
                        ElementMatchers.not(ElementMatchers.isConstructor())
                            .and(ElementMatchers.isAnnotatedWith(
                            ElementMatchers.named(ADDING_SPAN_ATTRIBUTES_ANNOTATION)
                            )
                        )
                    )
            )
            // Apply advice to constructors annotated with @AddingSpanAttributes
            .visit(
                Advice.to(AddingSpanAttributesConstructorAdvice::class.java)
                    .on(
                        ElementMatchers.isConstructor<MethodDescription>()
                            .and(ElementMatchers.isAnnotatedWith(
                                ElementMatchers.named(ADDING_SPAN_ATTRIBUTES_ANNOTATION)
                            )
                        )
                    )
            )
            // Apply advice to methods with parameters annotated with @SpanAttribute
            .visit(
                Advice.to(SpanAttributeMethodAdvice::class.java)
                    .on(
                        ElementMatchers.not(ElementMatchers.isConstructor())
                            .and(ElementMatchers.hasParameters(
                            ElementMatchers.whereAny(
                                ElementMatchers.isAnnotatedWith(
                                    ElementMatchers.named(SPAN_ATTRIBUTE_ANNOTATION)
                                    )
                                )
                            )
                        )
                    )
            )
            // Apply advice to constructors with parameters annotated with @SpanAttribute
            .visit(
                Advice.to(SpanAttributeConstructorAdvice::class.java)
                    .on(
                        ElementMatchers.isConstructor<MethodDescription>()
                            .and(ElementMatchers.hasParameters(
                                ElementMatchers.whereAny(
                                    ElementMatchers.isAnnotatedWith(
                                        ElementMatchers.named(SPAN_ATTRIBUTE_ANNOTATION)
                                    )
                                )
                            )
                        )
                    )
            )
    }

    override fun matches(target: TypeDescription?): Boolean {
        return target?.declaredMethods?.any { method ->
            method.declaredAnnotations.any { annotation ->
                annotation.annotationType.name == WITH_SPAN_ANNOTATION ||
                annotation.annotationType.name == ADDING_SPAN_ATTRIBUTES_ANNOTATION
            }
        } == true
    }

    override fun close() {
        // Nothing here yet?
    }
}