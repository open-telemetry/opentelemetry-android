/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp.v3_0.websocket;

import io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal.WebsocketListenerWrapper;
import net.bytebuddy.asm.Advice;
import okhttp3.WebSocketListener;

public class OkHttpClientWebsocketAdvice {

    @Advice.OnMethodEnter
    public static void enter(
            @Advice.Argument(value = 1, readOnly = false) WebSocketListener webSocketListener) {
        if (webSocketListener instanceof WebsocketListenerWrapper) return;

        webSocketListener = new WebsocketListenerWrapper(webSocketListener);
    }
}
