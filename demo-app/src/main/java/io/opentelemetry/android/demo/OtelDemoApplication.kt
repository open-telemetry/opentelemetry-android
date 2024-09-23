/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.AndroidAgent
import io.opentelemetry.android.agent.endpoint.EndpointConfig
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.events.EventBuilder
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider

const val TAG = "otel.demo"

class OtelDemoApplication : Application() {
    @SuppressLint("RestrictedApi")
    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "Initializing the opentelemetry-android-agent")

        val otelRumBuilder = AndroidAgent.createRumBuilder(
            this,
            endpointConfig = EndpointConfig.getDefault("http://10.0.2.2:4318"),
        ).setGlobalAttributes(Attributes.of(stringKey("toolkit"), "jetpack compose"))

        try {
            rum = otelRumBuilder.build()
            Log.d(TAG, "RUM session started: " + rum!!.rumSessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Oh no!", e)
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
