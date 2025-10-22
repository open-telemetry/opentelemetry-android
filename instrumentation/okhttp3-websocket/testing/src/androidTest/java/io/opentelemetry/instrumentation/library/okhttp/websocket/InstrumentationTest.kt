/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.websocket

import io.opentelemetry.android.test.common.OpenTelemetryRumRule
import io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal.WebsocketAttributes
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.NetworkAttributes
import io.opentelemetry.semconv.UrlAttributes
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.CountDownLatch

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class InstrumentationTest {
    @get:Rule
    var openTelemetryRumRule: OpenTelemetryRumRule = OpenTelemetryRumRule()

    private lateinit var webServer: MockWebServer
    private val client = OkHttpClient()

    @Before
    @Throws(IOException::class)
    fun setUp() {
        webServer = MockWebServer().apply(MockWebServer::start)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        webServer.close()
    }

    @Test
    @Throws(InterruptedException::class)
    fun websocketOpenEvents() {
        val lock = CountDownLatch(1)
        webServer.enqueue(MockResponse.Builder().webSocketUpgrade(webSocketListenerServer).build())
        val webSocketListenerClient: WebSocketListener =
            object : WebSocketListener() {
                override fun onOpen(
                    webSocket: WebSocket,
                    response: Response,
                ) {
                    lock.countDown()
                }
            }

        val request =
            Request
                .Builder()
                .get()
                .url(webServer.url("/"))
                .build()

        client.newWebSocket(request, webSocketListenerClient)
        lock.await()

        val logRecordItems = openTelemetryRumRule.inMemoryLogExporter.finishedLogRecordItems
        Assert.assertEquals(1, logRecordItems.size.toLong())

        val logRecordData = logRecordItems[0]
        val attributes = logRecordData.attributes

        Assert.assertNotNull(attributes.get(UrlAttributes.URL_FULL))
        Assert.assertNotNull(attributes.get(HttpAttributes.HTTP_REQUEST_METHOD))

        Assert.assertNotNull(attributes.get(NetworkAttributes.NETWORK_PEER_PORT))
        Assert.assertNotNull(attributes.get(NetworkAttributes.NETWORK_PEER_PORT))
        Assert.assertEquals(
            "websocket",
            attributes.get(NetworkAttributes.NETWORK_PROTOCOL_NAME),
        )
    }

    @Test
    @Throws(InterruptedException::class)
    fun websocketMessageEvent() {
        val lock = CountDownLatch(1)
        webServer.enqueue(MockResponse.Builder().webSocketUpgrade(webSocketListenerServer).build())
        val webSocketListenerClient: WebSocketListener =
            object : WebSocketListener() {
                override fun onMessage(
                    webSocket: WebSocket,
                    text: String,
                ) {
                    lock.countDown()
                }
            }

        val request =
            Request
                .Builder()
                .get()
                .url(webServer.url("/"))
                .build()

        val webSocket = client.newWebSocket(request, webSocketListenerClient)
        webSocket.send("hello")

        lock.await()
        val logRecordItems = openTelemetryRumRule.inMemoryLogExporter.finishedLogRecordItems
        Assert.assertEquals(2, logRecordItems.size.toLong())

        val logRecordData = logRecordItems[1]
        val attributes = logRecordData.attributes

        Assert.assertEquals("text", attributes.get(WebsocketAttributes.MESSAGE_TYPE))
        Assert.assertEquals(5L, attributes.get(WebsocketAttributes.MESSAGE_SIZE))
    }

    private val webSocketListenerServer: WebSocketListener =
        object : WebSocketListener() {
            override fun onMessage(
                webSocket: WebSocket,
                text: String,
            ) {
                webSocket.send(text)
            }
        }
}
