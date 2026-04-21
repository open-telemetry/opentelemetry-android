/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.websocket.internal

import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation

@AutoService(AndroidInstrumentation::class)
class OkHttpWebsocketInstrumentation : AndroidInstrumentation {
    override fun install(context: Context, openTelemetryRum: OpenTelemetryRum) {
        WebsocketListenerWrapper.configure(openTelemetryRum.openTelemetry)
    }

    override val name: String = "okhttp-websocket"
}
