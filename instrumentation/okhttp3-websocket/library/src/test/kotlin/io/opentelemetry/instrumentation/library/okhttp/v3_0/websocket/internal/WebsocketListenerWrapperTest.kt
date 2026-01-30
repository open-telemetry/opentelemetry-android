/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("ktlint:standard:package-name")

package io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal.WebsocketListenerWrapper.Companion.MESSAGE_SIZE
import io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal.WebsocketListenerWrapper.Companion.MESSAGE_TYPE
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.NetworkAttributes
import io.opentelemetry.semconv.UrlAttributes
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.encodeUtf8
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension

@ExtendWith(MockKExtension::class)
internal class WebsocketListenerWrapperTest {
    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @MockK
    lateinit var delegateListener: WebSocketListener

    @MockK
    lateinit var webSocket: WebSocket

    private lateinit var wrapper: WebsocketListenerWrapper

    private val testRequest =
        Request
            .Builder()
            .url("https://example.com:8080/socket")
            .method("GET", null)
            .build()

    @BeforeEach
    fun setup() {
        every { webSocket.request() } returns testRequest
        val installationContext = mockk<InstallationContext>()
        every { installationContext.openTelemetry } returns otelTesting.openTelemetry
        WebsocketListenerWrapper.configure(installationContext)
        wrapper = WebsocketListenerWrapper(delegateListener)
    }

    @Test
    fun testOnOpenEvent() {
        val response = buildMockResponse()
        every { delegateListener.onOpen(webSocket, response) } returns Unit
        wrapper.onOpen(webSocket, response)

        val logRecords = otelTesting.logRecords
        assertThat(logRecords).hasSize(1)

        val event = logRecords[0]
        assertThat(event)
            .hasEventName("websocket.open")
            .hasAttributesSatisfying(
                equalTo(NetworkAttributes.NETWORK_PROTOCOL_NAME, "websocket"),
                equalTo(HttpAttributes.HTTP_REQUEST_METHOD, "GET"),
                equalTo(UrlAttributes.URL_FULL, "https://example.com:8080/socket"),
                equalTo(NetworkAttributes.NETWORK_PEER_PORT, 8080L),
            )

        verify(exactly = 1) { delegateListener.onOpen(webSocket, response) }
    }

    @Test
    fun testOnMessageStringEvent() {
        val msg = "msg"
        every { delegateListener.onMessage(webSocket, msg) } returns Unit
        wrapper.onMessage(webSocket, msg)

        val logRecords = otelTesting.logRecords
        assertThat(logRecords).hasSize(1)

        val event = logRecords[0]
        assertThat(event)
            .hasEventName("websocket.message")
            .hasAttributesSatisfying(
                equalTo(NetworkAttributes.NETWORK_PROTOCOL_NAME, "websocket"),
                equalTo(HttpAttributes.HTTP_REQUEST_METHOD, "GET"),
                equalTo(UrlAttributes.URL_FULL, "https://example.com:8080/socket"),
                equalTo(NetworkAttributes.NETWORK_PEER_PORT, 8080L),
                equalTo(MESSAGE_TYPE, "text"),
                equalTo(MESSAGE_SIZE, msg.length.toLong()),
            )

        verify(exactly = 1) { delegateListener.onMessage(webSocket, msg) }
    }

    @Test
    fun testOnMessageByteEvent() {
        val bytesMessage = "Binary data".encodeUtf8()
        every { delegateListener.onMessage(webSocket, bytesMessage) } returns Unit
        wrapper.onMessage(webSocket, bytesMessage)

        val logRecords = otelTesting.logRecords
        assertThat(logRecords).hasSize(1)

        val event = logRecords[0]
        assertThat(event)
            .hasEventName("websocket.message")
            .hasAttributesSatisfying(
                equalTo(NetworkAttributes.NETWORK_PROTOCOL_NAME, "websocket"),
                equalTo(HttpAttributes.HTTP_REQUEST_METHOD, "GET"),
                equalTo(UrlAttributes.URL_FULL, "https://example.com:8080/socket"),
                equalTo(NetworkAttributes.NETWORK_PEER_PORT, 8080L),
                equalTo(MESSAGE_TYPE, "bytes"),
                equalTo(MESSAGE_SIZE, bytesMessage.size.toLong()),
            )

        verify(exactly = 1) { delegateListener.onMessage(webSocket, bytesMessage) }
    }

    @Test
    fun testOnClosedEvent() {
        val code = 1000
        val reason = "normal"
        every { delegateListener.onClosed(webSocket, code, reason) } returns Unit
        wrapper.onClosed(webSocket, code, reason)

        val logRecords = otelTesting.logRecords
        assertThat(logRecords).hasSize(1)

        val event = logRecords[0]
        assertThat(event)
            .hasEventName("websocket.close")
            .hasAttributesSatisfying(
                equalTo(NetworkAttributes.NETWORK_PROTOCOL_NAME, "websocket"),
                equalTo(HttpAttributes.HTTP_REQUEST_METHOD, "GET"),
                equalTo(UrlAttributes.URL_FULL, "https://example.com:8080/socket"),
                equalTo(NetworkAttributes.NETWORK_PEER_PORT, 8080L),
            )
        verify(exactly = 1) { delegateListener.onClosed(webSocket, code, reason) }
    }

    @Test
    fun testOnFailureEvent() {
        val throwable = RuntimeException("Connection failed")
        val response = buildMockResponse()
        every { delegateListener.onFailure(webSocket, throwable, response) } returns Unit
        wrapper.onFailure(webSocket, throwable, response)

        val logRecords = otelTesting.logRecords
        assertThat(logRecords).hasSize(1)

        val event = logRecords[0]
        assertThat(event)
            .hasEventName("websocket.error")
            .hasAttributesSatisfying(
                equalTo(NetworkAttributes.NETWORK_PROTOCOL_NAME, "websocket"),
                equalTo(HttpAttributes.HTTP_REQUEST_METHOD, "GET"),
                equalTo(UrlAttributes.URL_FULL, "https://example.com:8080/socket"),
                equalTo(NetworkAttributes.NETWORK_PEER_PORT, 8080L),
            )
        verify(exactly = 1) { delegateListener.onFailure(webSocket, throwable, response) }
    }

    private fun buildMockResponse(): Response =
        Response
            .Builder()
            .request(testRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(101)
            .message("Switching Protocols")
            .build()
}
