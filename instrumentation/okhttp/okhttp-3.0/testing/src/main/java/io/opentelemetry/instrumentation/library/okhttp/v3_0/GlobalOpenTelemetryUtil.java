/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;


import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class GlobalOpenTelemetryUtil {


    public static void setUpSpanExporter(SpanExporter spanExporter) {
        GlobalOpenTelemetry.resetForTest();
        GlobalOpenTelemetry.set(OpenTelemetrySdkUtil.createDefaultOpenTelemetrySdk(spanExporter));
    }

    public static void setSdkWithJaegerPropagator( ) {
        GlobalOpenTelemetry.resetForTest();
        GlobalOpenTelemetry.set(OpenTelemetrySdkUtil.createSdkWithJaegerPropagator());
    }


    public static void setSdkWithAllDefault( ) {
        GlobalOpenTelemetry.resetForTest();
        GlobalOpenTelemetry.set(OpenTelemetrySdkUtil.createSdkWithAllDefault());
    }


}
