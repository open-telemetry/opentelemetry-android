/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.opentelemetry.api.common.Attributes;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebsocketListenerWrapper extends WebSocketListener {
    private final WebSocketListener delegate;

    public WebsocketListenerWrapper(WebSocketListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Attributes attributes = WebsocketAttributeExtractor.extractAttributes(webSocket);
        WebsocketEventGenerator.generateEvent("onClosed", attributes);
        delegate.onClosed(webSocket, code, reason);
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Attributes attributes = WebsocketAttributeExtractor.extractAttributes(webSocket);
        WebsocketEventGenerator.generateEvent("onClosing", attributes);
        delegate.onClosing(webSocket, code, reason);
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        Attributes attributes = WebsocketAttributeExtractor.extractAttributes(webSocket);
        WebsocketEventGenerator.generateEvent("onOpen", attributes);
        delegate.onOpen(webSocket, response);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        Attributes attributes = WebsocketAttributeExtractor.extractAttributes(webSocket);
        WebsocketEventGenerator.generateEvent(
                "onMessage",
                attributes.toBuilder()
                        .put("message.type", "text")
                        .put("message.size", text.length())
                        .build());
        delegate.onMessage(webSocket, text);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
        Attributes attributes = WebsocketAttributeExtractor.extractAttributes(webSocket);
        WebsocketEventGenerator.generateEvent(
                "onMessage",
                attributes.toBuilder()
                        .put("message.type", "bytes")
                        .put("message.size", bytes.size())
                        .build());
        delegate.onMessage(webSocket, bytes);
    }

    @Override
    public void onFailure(
            @NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        Attributes attributes = WebsocketAttributeExtractor.extractAttributes(webSocket);
        WebsocketEventGenerator.generateEvent("onFailure", attributes);
        delegate.onFailure(webSocket, t, response);
    }
}
