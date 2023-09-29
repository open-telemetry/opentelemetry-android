/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
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
    private static InMemorySpanExporter spanExporter;

    @BeforeClass
    public static void setUpAll() {
        setUpInMemorySpanExporter();
    }

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        spanExporter.reset();
    }

    @Test
    public void okhttpTraces() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(200));

        Span span = getSpan();

        try (Scope ignored = span.makeCurrent()) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        SpanContext currentSpan = Span.current().getSpanContext();
                        assertEquals(span.getSpanContext().getTraceId(), currentSpan.getTraceId());
                        return chain.proceed(chain.request());
                    })
                    .build();
            createCall(client, "/test/").execute().close();
        }

        span.end();

        assertEquals(2, spanExporter.getFinishedSpanItems().size());
    }

    @Test
    public void okhttpTraces_with_callback() throws InterruptedException {
        CountDownLatch lock = new CountDownLatch(1);
        Span span = getSpan();

        try (Scope ignored = span.makeCurrent()) {
            server.enqueue(new MockResponse().setResponseCode(200));

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        SpanContext currentSpan = Span.current().getSpanContext();
                        // Verify context propagation.
                        assertEquals(span.getSpanContext().getTraceId(), currentSpan.getTraceId());
                        return chain.proceed(chain.request());
                    })
                    .build();
            createCall(client, "/test/").enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    // Verify that the original caller's context is the current one here.
                    assertEquals(span, Span.current());
                    lock.countDown();
                }
            });

        }

        lock.await();
        span.end();

        assertEquals(2, spanExporter.getFinishedSpanItems().size());
    }

    private static Span getSpan() {
        return GlobalOpenTelemetry.getTracer("TestTracer").spanBuilder("A Span").startSpan();
    }

    private static void setUpInMemorySpanExporter() {
        spanExporter = InMemorySpanExporter.create();
        OpenTelemetrySdk.builder()
                .setTracerProvider(getSimpleTracerProvider(spanExporter))
                .buildAndRegisterGlobal();
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
