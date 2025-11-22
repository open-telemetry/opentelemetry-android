/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.LoggerProvider
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.trace.Tracer

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
                    // To quickly observe session transitions in the demo app, you can temporarily
                    // reduce the timeout values here. For example:
                    // session {
                    //     backgroundInactivityTimeout = 30.seconds
                    //     maxLifetime = 2.minutes
                    // }
                    // Then, put the app in background for 30+ seconds or wait 2 minutes to see
                    // the session ID change. Be sure to revert these changes before committing.
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
