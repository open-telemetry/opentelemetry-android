package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import androidx.annotation.NonNull;

import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class OpenTelemetrySdkUtil {
    @NonNull
    public static OpenTelemetrySdk createDefaultOpenTelemetrySdk(SpanExporter spanExporter) {
        return OpenTelemetrySdkBuilderUtil.createRawBuilder()
                .setTracerProvider(SdkTracerProviderUtil.getSimpleTracerProvider(spanExporter))
                .build();
    }

    @NonNull
    public static OpenTelemetrySdk createSdkWithJaegerPropagator( ) {
        return OpenTelemetrySdkBuilderUtil.createRawBuilder()
                .setPropagators(ContextPropagators.create(JaegerPropagator.getInstance()))
                .build();
    }
    @NonNull
    public static OpenTelemetrySdk createSdkWithAllDefault( ) {
        return OpenTelemetrySdkBuilderUtil.createRawBuilder()
                .build();
    }
}
