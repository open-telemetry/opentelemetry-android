/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter

const val TAG = "otel.demo"

class OtelDemoApplication : Application() {
    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Initializing the opentelemetry-android-agent")
        val diskBufferingConfig = DiskBufferingConfig(
            enabled = true, maxCacheSize = 10_000_000, debugEnabled = true);
        val config =
            OtelRumConfig()
                .setGlobalAttributes(Attributes.of(stringKey("toolkit"), "jetpack compose"))
                .setDiskBufferingConfig(diskBufferingConfig)

        // 10.0.2.2 is apparently a special binding to the host running the emulator
        try {
            rum = OpenTelemetryRumInitializer.initialize(
                application = this,
                endpointBaseUrl = "http://10.0.2.2:4318",
                rumConfig = config
            )
            Log.d(TAG, "RUM session started: " + rum!!.rumSessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Oh no!", e)
        }

        // This is needed to get R8 missing rules warnings.
        initializeOtelWithGrpc()

        createAliveCounter()
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

    // A simple counter that merely logs the number of seconds that the app has been
    // alive. This is simply used to demonstrate the metrics signal before other
    // meaningful metrics have been created.
    private fun createAliveCounter() {
        val startTime = System.currentTimeMillis()
        rum?.openTelemetry
            ?.getMeter("android.lifetime")
            ?.counterBuilder("app.uptime.seconds")
            ?.setDescription("The number of seconds the app has been alive.")
            ?.setUnit("s")
            ?.buildWithCallback {
                measurement -> measurement.record((System.currentTimeMillis() - startTime)/1000)
            }
    }

    companion object {
        var rum: OpenTelemetryRum? = null

        fun tracer(name: String): Tracer? {
            return rum?.openTelemetry?.tracerProvider?.get(name)
        }

        fun counter(name: String): LongCounter? {
            return rum?.openTelemetry?.meterProvider?.get("demo.app")?.counterBuilder(name)?.build()
        }

        fun eventBuilder(scopeName: String, eventName: String): LogRecordBuilder {
            val logger = rum?.openTelemetry?.logsBridge?.loggerBuilder(scopeName)?.build()
            var builder: ExtendedLogRecordBuilder = logger?.logRecordBuilder() as ExtendedLogRecordBuilder
            return builder.setEventName(eventName)
        }
    }
}
