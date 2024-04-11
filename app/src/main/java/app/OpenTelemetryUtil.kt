package app

import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.extension.trace.propagation.JaegerPropagator
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import timber.log.Timber

object OpenTelemetryUtil {


    fun kickOffRootSpan(openTelemetryRum: OpenTelemetryRum): Span {
        val tracer: Tracer = openTelemetryRum.openTelemetry.getTracer(DemoApp.TRACER)
        val spanBuilder: SpanBuilder = tracer.spanBuilder(DemoApp.SPAN_COLD_LAUNCH)
        val baggage = Baggage.builder().put("user_name", "tonytang").build()
        val scope: Scope = Context.current().with(baggage).makeCurrent()
        scope.use {
            spanBuilder.setAttribute("root_key_1", "root_key_2")
            spanBuilder.setSpanKind(SpanKind.CLIENT)
            val coldLaunchSpan: Span = spanBuilder.startSpan()
            coldLaunchSpan.addEvent("started_event")
            val spanContext = coldLaunchSpan.spanContext
            val traceId = spanContext.traceId
            val spanId = spanContext.spanId
            Timber.tag(DemoApp.SPAN_COLD_LAUNCH).i("root_span_traceId:trace_id:%s,span_id: %s", traceId, spanId)
            return coldLaunchSpan
        }

    }

     fun configOpenTelemetry(spanExporter:SpanExporter) {
        //Make `uber-trace-id` attached.
        val jaegerPropagator: JaegerPropagator = JaegerPropagator.getInstance()
        val contextPropagators = ContextPropagators.create(jaegerPropagator)
        val spanProcessor = SimpleSpanProcessor.create(spanExporter)
        val tracer = SdkTracerProvider.builder().addSpanProcessor(spanProcessor).build()
        val telemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(tracer)
                .setPropagators(contextPropagators)
                .build()
        GlobalOpenTelemetry.set(telemetrySdk)
    }




}