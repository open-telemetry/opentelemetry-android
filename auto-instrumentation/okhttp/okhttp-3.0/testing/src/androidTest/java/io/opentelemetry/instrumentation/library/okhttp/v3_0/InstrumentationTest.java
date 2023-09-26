/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class InstrumentationTest {
    private MockWebServer server;

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    public void okhttpTraces() throws IOException {
        InMemorySpanExporter spanExporter = getInMemorySpanExporter();

        server.enqueue(new MockResponse().setResponseCode(200));

        OkHttpClient client = new OkHttpClient();
        createCall(client, "/test/").execute();

        assertEquals(1, spanExporter.getFinishedSpanItems().size());
    }

    @Test
    public void okhttpTraces_with_callback() throws InterruptedException {
        CountDownLatch lock = new CountDownLatch(1);
        InMemorySpanExporter spanExporter = getInMemorySpanExporter();
        Span span = GlobalOpenTelemetry.getTracer("TestTracer").spanBuilder("A Span").startSpan();

        try (Scope ignored = span.makeCurrent()) {
            server.enqueue(new MockResponse().setResponseCode(200));

            OkHttpClient client = new OkHttpClient();
            createCall(client, "/test/").enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    assertEquals(Context.root().with(span), Context.current());
                    lock.countDown();
                }
            });

        }

        lock.await();

        assertEquals(1, spanExporter.getFinishedSpanItems().size());
    }

    @NonNull
    private static InMemorySpanExporter getInMemorySpanExporter() {
        InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
        OpenTelemetrySdk.builder()
                .setTracerProvider(getSimpleTracerProvider(spanExporter))
                .buildAndRegisterGlobal();
        return spanExporter;
    }

    private Call createCall(OkHttpClient client, String urlPath) {
        Request request = new Request.Builder().url(server.url(urlPath)).build();
        return client.newCall(request);
    }

    @NonNull
    private static SdkTracerProvider getSimpleTracerProvider(InMemorySpanExporter spanExporter) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
    }
}
