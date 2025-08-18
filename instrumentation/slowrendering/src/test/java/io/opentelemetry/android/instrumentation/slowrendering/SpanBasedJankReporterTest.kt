package io.opentelemetry.android.instrumentation.slowrendering

import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach

class SpanBasedJankReporterTest {

    private lateinit var tracer: Tracer

    @Rule
    var otelTesting: OpenTelemetryRule = OpenTelemetryRule.create()

    @BeforeEach
    fun setup(){
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
    }

}