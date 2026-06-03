/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.context.propagation.ContextPropagators

/**
 * A wrapper around [delegate] that provides no-op implementations for the providers of each API that are disabled.
 */
internal class DisableableOpenTelemetry(
    private val delegate: OpenTelemetry,
    private val tracingEnabled: Boolean,
    private val loggingEnabled: Boolean,
    private val metricsEnabled: Boolean,
) : OpenTelemetry {
    override fun getTracerProvider(): TracerProvider =
        if (tracingEnabled) {
            delegate.tracerProvider
        } else {
            TracerProvider.noop()
        }

    override fun getLogsBridge(): LoggerProvider =
        if (loggingEnabled) {
            delegate.logsBridge
        } else {
            LoggerProvider.noop()
        }

    override fun getMeterProvider(): MeterProvider =
        if (metricsEnabled) {
            delegate.meterProvider
        } else {
            MeterProvider.noop()
        }

    override fun getPropagators(): ContextPropagators = delegate.propagators
}
