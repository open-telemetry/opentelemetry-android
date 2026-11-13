/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.minversions.weaving

import android.app.Application
import android.util.Log
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URL

/**
 * Gives each agent something to weave. The log and HttpURLConnection agents rewrite the calls made
 * here, while the OkHttp agents rewrite OkHttp's own classes.
 */
class WeavingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenTelemetryRumInitializer.initialize(this)
    }

    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    fun openWebSocket(url: String): WebSocket =
        okHttpClient().newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {})

    fun connect(url: String) = URL(url).openConnection().connect()

    fun log(message: String) = Log.v("WeavingApplication", message)
}
