/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.websocket.internal

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext

@AutoService(AndroidInstrumentation::class)
class OkHttpWebsocketInstrumentation : AndroidInstrumentation {
    override fun install(ctx: InstallationContext) {
        WebsocketListenerWrapper.configure(ctx)
    }

    override val name: String = "okhttp-websocket"
}
