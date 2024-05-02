package io.opentelemetry.android.demo

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.OpenTelemetryRumBuilder
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter

private const val TAG = "otel.demo"

class OtelSampleApplication : Application() {

    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Initializing the opentelemetry-android-agent")
        val diskBufferingConfig = DiskBufferingConfiguration.builder()
            .setEnabled(true)
            .setMaxCacheSize(10_000_000)
            .build()
        val config = OtelRumConfig()
            .setGlobalAttributes(Attributes.of(stringKey("toolkit"), "jetpack compose"))
            .setDiskBufferingConfiguration(diskBufferingConfig)

        // 10.0.2.2 is apparently a special binding to the host running the emulator
        val ingestUrl = "http://10.0.2.2:4318/v1/traces"
        val otelRumBuilder: OpenTelemetryRumBuilder = OpenTelemetryRum.builder(this, config)
            .addSpanExporterCustomizer {
                OtlpHttpSpanExporter.builder()
                    .setEndpoint(ingestUrl)
                    .build()
            }
        try {
            rum = otelRumBuilder.build()
            Log.d(TAG,  "RUM session started: " + rum!!.rumSessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Oh no!", e)
        }
    }

    companion object {
        var rum: OpenTelemetryRum? = null
        fun tracer(name: String): Tracer? {
            return rum?.openTelemetry?.tracerProvider?.get(name)
        }
    }
}