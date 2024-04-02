package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;

public class OpenTelemetrySdkBuilderUtil {
    public static OpenTelemetrySdkBuilder createRawBuilder() {
        return OpenTelemetrySdk.builder();
    }
}
