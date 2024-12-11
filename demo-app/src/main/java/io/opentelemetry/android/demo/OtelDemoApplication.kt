/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.OpenTelemetryRumBuilder
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration
import io.opentelemetry.android.instrumentation.sessions.SessionInstrumentation
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.events.EventBuilder
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider

const val TAG = "otel.demo"

class OtelDemoApplication : Application() {
    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Initializing the opentelemetry-android-agent")
        val diskBufferingConfig =
            DiskBufferingConfiguration.builder()
                .setEnabled(true)
                .setMaxCacheSize(10_000_000)
                .build()
        val config =
            OtelRumConfig()
                .setGlobalAttributes(Attributes.of(stringKey("toolkit"), "jetpack compose"))
                .setDiskBufferingConfiguration(diskBufferingConfig)

        // 10.0.2.2 is apparently a special binding to the host running the emulator
        val spansIngestUrl = "http://10.0.2.2:4318/v1/traces"
        val logsIngestUrl = "http://10.0.2.2:4318/v1/logs"
        val otelRumBuilder: OpenTelemetryRumBuilder =
            OpenTelemetryRum.builder(this, config)
                .addSpanExporterCustomizer {
                    OtlpHttpSpanExporter.builder()
                        .setEndpoint(spansIngestUrl)
                        .build()
                }
                .addLogRecordExporterCustomizer {
                    OtlpHttpLogRecordExporter.builder()
                        .setEndpoint(logsIngestUrl)
                        .build()
                }
                // TODO: This should NOT be necessary if it's in the runtime classpath...
                .addInstrumentation(SessionInstrumentation())
        try {
            rum = otelRumBuilder.build()
            Log.d(TAG, "RUM session started: " + rum!!.rumSessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Oh no!", e)
        }

        // This is needed to get R8 missing rules warnings.
        initializeOtelWithGrpc()
    }

    // This is not used but it's needed to verify that our consumer proguard rules cover this use case.
    private fun initializeOtelWithGrpc() {
        val builder = OpenTelemetryRum.builder(this)
            .addSpanExporterCustomizer {
                OtlpGrpcSpanExporter.builder().build()
            }
            .addLogRecordExporterCustomizer {
                OtlpGrpcLogRecordExporter.builder().build()
            }

        // This is an overly-cautious measure to prevent R8 from discarding away the whole method
        // in case it identifies that it's actually not doing anything meaningful.
        if (System.currentTimeMillis() < 0) {
            print(builder)
        }
    }

    companion object {
        var rum: OpenTelemetryRum? = null

        fun tracer(name: String): Tracer? {
            return rum?.openTelemetry?.tracerProvider?.get(name)
        }

        fun eventBuilder(scopeName: String, eventName: String): EventBuilder {
            val loggerProvider = rum?.openTelemetry?.logsBridge
            val eventLogger =
                SdkEventLoggerProvider.create(loggerProvider).get(scopeName)
            return eventLogger.builder(eventName)
        }
    }
}
