/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import io.opentelemetry.android.test.common.OpenTelemetryTestUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
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
    private static final OpenTelemetryTestUtils openTelemetryTestUtils =
            new OpenTelemetryTestUtils();

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
        openTelemetryTestUtils.setUpSpanExporter(inMemorySpanExporter);
        server.enqueue(new MockResponse().setResponseCode(200));

        Span span = openTelemetryTestUtils.getSpan();

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
        openTelemetryTestUtils.setUpSpanExporter(inMemorySpanExporter);
        CountDownLatch lock = new CountDownLatch(1);
        Span span = openTelemetryTestUtils.getSpan();

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
        // NOTE: For some reason this test always passes when running all the tests in this file at
        // once,
        // so it should be run isolated to actually get it to fail when it's expected to fail.
        OtlpHttpSpanExporter exporter =
                OtlpHttpSpanExporter.builder().setEndpoint(server.url("").toString()).build();
        openTelemetryTestUtils.setUpSpanExporter(exporter);

        server.enqueue(new MockResponse().setResponseCode(200));

        // This span should trigger 1 export okhttp call, which is the only okhttp call expected
        // for this test case.
        openTelemetryTestUtils.getSpan().end();

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

    private Call createCall(OkHttpClient client, String urlPath) {
        Request request = new Request.Builder().url(server.url(urlPath)).build();
        return client.newCall(request);
    }
}
