/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class InstrumentationTest {
    private MockWebServer server;
    private static final InMemorySpanExporter inMemorySpanExporter = InMemorySpanExporter.create();

    @Before
    public void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
        inMemorySpanExporter.reset();
    }

    @Test
    public void okhttpTraces() throws IOException {
        setUpSpanExporter(inMemorySpanExporter);
        server.enqueue(new MockResponse().setResponseCode(200));

        Span span = getSpan();

        try (Scope ignored = span.makeCurrent()) {
            OkHttpClient client =
                    new OkHttpClient.Builder()
                            .addInterceptor(
                                    chain -> {
                                        SpanContext currentSpan = Span.current().getSpanContext();
                                        assertEquals(
                                                span.getSpanContext().getTraceId(),
                                                currentSpan.getTraceId());
                                        return chain.proceed(chain.request());
                                    })
                            .build();
            createCall(client, "/test/").execute().close();
        }

        span.end();

        assertEquals(2, inMemorySpanExporter.getFinishedSpanItems().size());
    }

    @Test
    public void okhttpTraces_with_callback() throws InterruptedException {
        setUpSpanExporter(inMemorySpanExporter);
        CountDownLatch lock = new CountDownLatch(1);
        Span span = getSpan();

        try (Scope ignored = span.makeCurrent()) {
            server.enqueue(new MockResponse().setResponseCode(200));

            OkHttpClient client =
                    new OkHttpClient.Builder()
                            .addInterceptor(
                                    chain -> {
                                        SpanContext currentSpan = Span.current().getSpanContext();
                                        // Verify context propagation.
                                        assertEquals(
                                                span.getSpanContext().getTraceId(),
                                                currentSpan.getTraceId());
                                        return chain.proceed(chain.request());
                                    })
                            .build();
            createCall(client, "/test/")
                    .enqueue(
                            new Callback() {
                                @Override
                                public void onFailure(@NonNull Call call, @NonNull IOException e) {}

                                @Override
                                public void onResponse(
                                        @NonNull Call call, @NonNull Response response) {
                                    // Verify that the original caller's context is the current one
                                    // here.
                                    assertEquals(span, Span.current());
                                    lock.countDown();
                                }
                            });
        }

        lock.await();
        span.end();

        assertEquals(2, inMemorySpanExporter.getFinishedSpanItems().size());
    }

    @Test
    public void avoidCreatingSpansForInternalOkhttpRequests() throws InterruptedException {
        OtlpHttpSpanExporter exporter =
                OtlpHttpSpanExporter.builder().setEndpoint(server.url("").toString()).build();
        setUpSpanExporter(exporter);

        server.enqueue(new MockResponse().setResponseCode(200));

        // This span should trigger 1 export okhttp call, which is the only okhttp call expected
        // for this test case.
        getSpan().end();

        // Wait for unwanted extra okhttp requests.
        int loop = 0;
        while (loop < 10) {
            Thread.sleep(100);
            // Stop waiting if we get at least one unwanted request.
            if (server.getRequestCount() > 1) {
                break;
            }
            loop++;
        }

        assertEquals(1, server.getRequestCount());
    }

    private static Span getSpan() {
        return GlobalOpenTelemetry.get().getTracer("TestTracer").spanBuilder("A Span").startSpan();
    }

    private void setUpSpanExporter(SpanExporter spanExporter) {
        OpenTelemetrySdk openTelemetry =
                OpenTelemetrySdk.builder()
                        .setTracerProvider(getSimpleTracerProvider(spanExporter))
                        .build();
        GlobalOpenTelemetry.resetForTest();
        GlobalOpenTelemetry.set(openTelemetry);
    }

    private Call createCall(OkHttpClient client, String urlPath) {
        Request request = new Request.Builder().url(server.url(urlPath)).build();
        return client.newCall(request);
    }

    @NonNull
    private SdkTracerProvider getSimpleTracerProvider(SpanExporter spanExporter) {
        return SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();
    }
}
