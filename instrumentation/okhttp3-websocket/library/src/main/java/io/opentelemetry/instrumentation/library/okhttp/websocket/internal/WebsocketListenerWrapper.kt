/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.websocket.internal

import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.NetworkAttributes
import io.opentelemetry.semconv.UrlAttributes
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebsocketListenerWrapper(
    private val delegate: WebSocketListener,
) : WebSocketListener() {
    override fun onClosed(
        webSocket: WebSocket,
        code: Int,
        reason: String,
    ) {
        val attributes = extractAttributes(webSocket)
        emitEvent("websocket.close", attributes)
        delegate.onClosed(webSocket, code, reason)
    }

    override fun onOpen(
        webSocket: WebSocket,
        response: Response,
    ) {
        val attributes = extractAttributes(webSocket)
        emitEvent("websocket.open", attributes)
        delegate.onOpen(webSocket, response)
    }

    override fun onMessage(
        webSocket: WebSocket,
        text: String,
    ) {
        val attributes = extractAttributes(webSocket)
        emitEvent(
            "websocket.message",
            attributes
                .toBuilder()
                .put(MESSAGE_TYPE, "text")
                .put(MESSAGE_SIZE, text.length.toLong())
                .build(),
        )
        delegate.onMessage(webSocket, text)
    }

    override fun onMessage(
        webSocket: WebSocket,
        bytes: ByteString,
    ) {
        val attributes = extractAttributes(webSocket)
        emitEvent(
            "websocket.message",
            attributes
                .toBuilder()
                .put(MESSAGE_TYPE, "bytes")
                .put(MESSAGE_SIZE, bytes.size.toLong())
                .build(),
        )
        delegate.onMessage(webSocket, bytes)
    }

    override fun onFailure(
        webSocket: WebSocket,
        t: Throwable,
        response: Response?,
    ) {
        val attributes = extractAttributes(webSocket)
        emitEvent("websocket.error", attributes)
        delegate.onFailure(webSocket, t, response)
    }

    private fun extractAttributes(socket: WebSocket): Attributes =
        Attributes
            .builder()
            .apply {
                val request = socket.request()
                put(NetworkAttributes.NETWORK_PROTOCOL_NAME, "websocket")
                put(HttpAttributes.HTTP_REQUEST_METHOD, request.method)
                put(UrlAttributes.URL_FULL, request.url.toString())
                put(NetworkAttributes.NETWORK_PEER_PORT, request.url.port.toLong())
            }.build()

    private fun emitEvent(
        eventName: String,
        attributes: Attributes,
    ) {
        logger
            .logRecordBuilder()
            .setEventName(eventName)
            .setAllAttributes(attributes)
            .emit()
    }

    companion object {
        private const val SCOPE = "io.opentelemetry.websocket.events"

        val MESSAGE_SIZE: AttributeKey<Long> = AttributeKey.longKey("websocket.message.size")
        val MESSAGE_TYPE: AttributeKey<String> = AttributeKey.stringKey("websocket.message.type")

        private var logger: Logger =
            OpenTelemetry
                .noop()
                .logsBridge
                .loggerBuilder(SCOPE)
                .build()

        @JvmStatic
        fun configure(context: InstallationContext) {
            logger =
                context.openTelemetry.logsBridge
                    .loggerBuilder(SCOPE)
                    .build()
        }
    }
}
