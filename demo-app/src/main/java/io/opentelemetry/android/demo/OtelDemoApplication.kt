/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor
import kotlin.random.Random

const val TAG = "otel.demo"

class OtelDemoApplication : Application() {

    @OptIn(Incubating::class)
    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Initializing the opentelemetry-android-agent")

        // 10.0.2.2 is a special binding to the host running the emulator
        try {
            rum = OpenTelemetryRumInitializer.initialize(
                context = this@OtelDemoApplication,
                configuration = {
                    httpExport {
                        baseUrl = "http://10.0.2.2:4318"
                    }
                    globalAttributes {
                        Attributes.of(stringKey("toolkit"), "jetpack compose")
                    }
                    otelSdkCustomizations {
                        customizeTracerProvider { builder ->
                            builder.addSpanProcessor(OtelDemoCustomSpanProcessor())
                        }
                        customizeLoggerProvider { builder ->
                            builder.addLogRecordProcessor { _, logRecord ->
                                // onEmit()
                                logRecord.setAttribute(stringKey("awesome"), "sauce-" + Random.nextInt())
                            }
                        }
                        customizeMeterProvider { builder -> builder /* customize as you like! */ }
                    }
                }
            )
            Log.d(TAG, "RUM session started: " + rum?.getRumSessionId())
        } catch (e: Exception) {
            Log.e(TAG, "Oh no!", e)
        }
    }

    companion object {
        var rum: OpenTelemetryRum? = null

        fun tracer(name: String): Tracer? {
            return rum?.openTelemetry?.tracerProvider?.get(name)
        }

        fun counter(name: String): LongCounter? {
            return rum?.openTelemetry?.meterProvider?.get("demo.app")?.counterBuilder(name)
                ?.build()
        }

        fun eventBuilder(scopeName: String, eventName: String): LogRecordBuilder {
            if (rum == null) {
                return LoggerProvider.noop().get("noop").logRecordBuilder()
            }
            val logger = rum!!.openTelemetry.logsBridge.loggerBuilder(scopeName).build()
            return logger.logRecordBuilder().setEventName(eventName)
        }
    }
}

class OtelDemoCustomSpanProcessor: SpanProcessor {
    override fun onStart(
        parentContext: Context,
        span: ReadWriteSpan
    ) {
        span.setAttribute("my.custom.attribute", Random.nextLong())
    }

    override fun isStartRequired(): Boolean = true

    override fun isEndRequired(): Boolean = false

    override fun onEnd(span: ReadableSpan) {
        //no-op
    }
}
