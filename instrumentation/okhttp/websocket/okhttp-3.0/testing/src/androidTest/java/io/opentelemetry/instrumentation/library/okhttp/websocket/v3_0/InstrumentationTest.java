/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.websocket.v3_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.annotation.NonNull;
import io.opentelemetry.android.test.common.OpenTelemetryRumRule;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.library.okhttp.v3_0.websocket.internal.WebsocketAttributes;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class InstrumentationTest {
    private MockWebServer webServer;

    private final OkHttpClient client = new OkHttpClient();

    @Rule public OpenTelemetryRumRule openTelemetryRumRule = new OpenTelemetryRumRule();

    @Before
    public void setUp() throws IOException {
        webServer = new MockWebServer();
        webServer.start();
    }

    @After
    public void tearDown() throws IOException {
        webServer.shutdown();
    }

    @Test
    public void websocketOpenEvents() throws InterruptedException {
        CountDownLatch lock = new CountDownLatch(1);
        webServer.enqueue(new MockResponse().withWebSocketUpgrade(webSocketListenerSever));
        WebSocketListener webSocketListenerClient =
                new WebSocketListener() {
                    @Override
                    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                        lock.countDown();
                    }
                };

        Request request = new Request.Builder().get().url(webServer.url("/")).build();

        client.newWebSocket(request, webSocketListenerClient);
        lock.await();

        List<LogRecordData> logRecordItems =
                openTelemetryRumRule.inMemoryLogExporter.getFinishedLogRecordItems();
        assertEquals(1, logRecordItems.size());

        LogRecordData logRecordData = logRecordItems.get(0);
        Attributes attributes = logRecordData.getAttributes();

        assertNotNull(attributes.get(UrlAttributes.URL_FULL));
        assertNotNull(attributes.get(HttpAttributes.HTTP_REQUEST_METHOD));

        assertNotNull(attributes.get(NetworkAttributes.NETWORK_PEER_PORT));
        assertNotNull(attributes.get(NetworkAttributes.NETWORK_PEER_PORT));
        assertEquals("websocket", attributes.get(NetworkAttributes.NETWORK_PROTOCOL_NAME));
    }

    @Test
    public void websocketMessageEvent() throws InterruptedException {
        CountDownLatch lock = new CountDownLatch(1);
        webServer.enqueue(new MockResponse().withWebSocketUpgrade(webSocketListenerSever));
        WebSocketListener webSocketListenerClient =
                new WebSocketListener() {
                    @Override
                    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                        lock.countDown();
                    }
                };

        Request request = new Request.Builder().get().url(webServer.url("/")).build();

        WebSocket webSocket = client.newWebSocket(request, webSocketListenerClient);
        webSocket.send("hello");

        lock.await();
        List<LogRecordData> logRecordItems =
                openTelemetryRumRule.inMemoryLogExporter.getFinishedLogRecordItems();
        assertEquals(2, logRecordItems.size());

        LogRecordData logRecordData = logRecordItems.get(1);
        Attributes attributes = logRecordData.getAttributes();

        assertEquals("text", attributes.get(WebsocketAttributes.MESSAGE_TYPE));
        assertEquals(Long.valueOf(5), attributes.get(WebsocketAttributes.MESSAGE_SIZE));
    }

    private final WebSocketListener webSocketListenerSever =
            new WebSocketListener() {
                @Override
                public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                    webSocket.send(text);
                }
            };
}
