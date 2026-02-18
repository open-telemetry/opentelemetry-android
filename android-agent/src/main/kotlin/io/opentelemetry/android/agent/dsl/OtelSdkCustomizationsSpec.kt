/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder

typealias TracerProviderCustomizer = Function1<SdkTracerProviderBuilder, SdkTracerProviderBuilder>
typealias LoggerProviderCustomizer = Function1<SdkLoggerProviderBuilder, SdkLoggerProviderBuilder>
typealias MeterProviderCustomizer = Function1<SdkMeterProviderBuilder, SdkMeterProviderBuilder>

@OpenTelemetryDslMarker
class OtelSdkCustomizationsSpec internal constructor() {
    internal val tracerProviderCustomizers: MutableList<TracerProviderCustomizer> = mutableListOf()
    internal val loggerProviderCustomizers: MutableList<LoggerProviderCustomizer> = mutableListOf()
    internal val meterProviderCustomizers: MutableList<MeterProviderCustomizer> = mutableListOf()

    /**
     * Modify the creation of the OpenTelemetry TracerProvider by providing
     * your own customizer here. The customizer is a function
     * that receives an instance of [SdkTracerProviderBuilder] and returns an
     * instance of [SdkTracerProviderBuilder], which should almost always be
     * the same instance. If a new instance is returned, the operation can be
     * destructive.
     */
    fun customizeTracerProvider(customizer: TracerProviderCustomizer) {
        tracerProviderCustomizers.add(customizer)
    }

    /**
     * Modify the creation of the OpenTelemetry LoggerProvider by providing
     * your own customizer here. The customizer is a function
     * that receives an instance of [SdkLoggerProviderBuilder] and returns an
     * instance of [SdkLoggerProviderBuilder], which should almost always be
     * the same instance. If a new instance is returned, the operation can be
     * destructive.
     */
    fun customizeLoggerProvider(customizer: LoggerProviderCustomizer) {
        loggerProviderCustomizers.add(customizer)
    }

    /**
     * Modify the creation of the OpenTelemetry MeterProvider by providing
     * your own customizer here. The customizer is a function
     * that receives an instance of [SdkMeterProviderBuilder] and returns an
     * instance of [SdkMeterProviderBuilder], which should almost always be
     * the same instance. If a new instance is returned, the operation can be
     * destructive.
     */
    fun customizeMeterProvider(customizer: MeterProviderCustomizer) {
        meterProviderCustomizers.add(customizer)
    }
}
