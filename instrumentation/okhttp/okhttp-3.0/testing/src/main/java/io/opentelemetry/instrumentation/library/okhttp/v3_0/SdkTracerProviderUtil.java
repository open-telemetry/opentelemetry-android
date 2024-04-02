package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import androidx.annotation.NonNull;

import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class SdkTracerProviderUtil {
    @NonNull
    public static SdkTracerProvider getSimpleTracerProvider(SpanExporter spanExporter) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
    }
}
