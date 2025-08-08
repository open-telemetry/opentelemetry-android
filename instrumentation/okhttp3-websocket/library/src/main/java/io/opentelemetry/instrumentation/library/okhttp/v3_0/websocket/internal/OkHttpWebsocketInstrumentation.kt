/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("ktlint:standard:package-name")

package io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext

@AutoService(AndroidInstrumentation::class)
class OkHttpWebsocketInstrumentation : AndroidInstrumentation {
    override fun install(ctx: InstallationContext) {
        WebsocketEventGenerator.configure(ctx)
    }

    override val name: String = "okhttp-websocket"
}
