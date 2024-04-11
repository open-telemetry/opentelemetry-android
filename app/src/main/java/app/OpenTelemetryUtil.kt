package app

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter

object OpenTelemetryUtil {

    fun configOpenTelemetry(spanExporter:SpanExporter) {
        val jaegerPropagator: JaegerPropagator = JaegerPropagator.getInstance()
        val contextPropagators = ContextPropagators.create(jaegerPropagator)
        val spanProcessor = SimpleSpanProcessor.create(spanExporter)
        val tracer = SdkTracerProvider.builder().addSpanProcessor(spanProcessor).build()
        GlobalOpenTelemetry.set(OpenTelemetrySdk.builder().setTracerProvider(tracer)
                .setPropagators(contextPropagators)
                .build())
    }
}