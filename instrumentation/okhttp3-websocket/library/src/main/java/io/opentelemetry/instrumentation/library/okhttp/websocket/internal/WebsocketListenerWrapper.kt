/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.websocket.internal

import io.opentelemetry.android.semconv.events.WebsocketCloseEvent
import io.opentelemetry.android.semconv.events.WebsocketErrorEvent
import io.opentelemetry.android.semconv.events.WebsocketMessageEvent
import io.opentelemetry.android.semconv.events.WebsocketOpenEvent
import io.opentelemetry.api.OpenTelemetry
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
        WebsocketCloseEvent().emit(logger, attributes)
        delegate.onClosed(webSocket, code, reason)
    }

    override fun onOpen(
        webSocket: WebSocket,
        response: Response,
    ) {
        val attributes = extractAttributes(webSocket)
        WebsocketOpenEvent().emit(logger, attributes)
        delegate.onOpen(webSocket, response)
    }

    override fun onMessage(
        webSocket: WebSocket,
        text: String,
    ) {
        val attributes = extractAttributes(webSocket)
        WebsocketMessageEvent(
            websocketMessageSize = text.length.toLong(),
            websocketMessageType = "text",
        ).emit(logger, attributes)
        delegate.onMessage(webSocket, text)
    }

    override fun onMessage(
        webSocket: WebSocket,
        bytes: ByteString,
    ) {
        val attributes = extractAttributes(webSocket)
        WebsocketMessageEvent(
            websocketMessageSize = bytes.size.toLong(),
            websocketMessageType = "bytes",
        ).emit(logger, attributes)
        delegate.onMessage(webSocket, bytes)
    }

    override fun onFailure(
        webSocket: WebSocket,
        t: Throwable,
        response: Response?,
    ) {
        val attributes = extractAttributes(webSocket)
        WebsocketErrorEvent().emit(logger, attributes)
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

    companion object {
        private const val SCOPE = "io.opentelemetry.websocket.events"

        private var logger: Logger =
            OpenTelemetry
                .noop()
                .logsBridge
                .loggerBuilder(SCOPE)
                .build()

        @JvmStatic
        fun configure(openTelemetry: OpenTelemetry) {
            logger =
                openTelemetry.logsBridge
                    .loggerBuilder(SCOPE)
                    .build()
        }
    }
}
