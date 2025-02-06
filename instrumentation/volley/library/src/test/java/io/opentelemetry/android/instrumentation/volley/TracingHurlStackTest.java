/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.incubating.HttpIncubatingAttributes;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TracingHurlStackTest {

    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private TestRequestQueue testQueue;
    private StuckTestHelper stuckTestHelper;
    private MockWebServer server;

    @Before
    public void setup() {
        // setup Volley with TracingHurlStack
        HurlStack tracingHurlStack =
                VolleyTracing.create(otelTesting.getOpenTelemetry()).newHurlStack();
        testQueue = TestRequestQueue.create(tracingHurlStack);
        stuckTestHelper = StuckTestHelper.start();

        Logger.getLogger(MockWebServer.class.getName()).setLevel(Level.WARNING);
        // setup test server
        server = new MockWebServer();
    }

    @After
    public void cleanup() throws IOException {
        stuckTestHelper.close();
        server.shutdown();
    }

    @Test
    public void success()
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        String responseBody = "success";
        server.enqueue(new MockResponse().setBody(responseBody));
        server.play();

        URL url = server.getUrl("/success");

        RequestFuture<String> response = RequestFuture.newFuture();
        StringRequest stringRequest =
                new StringRequest(Request.Method.GET, url.toString(), response, response);

        testQueue.addToQueue(stringRequest);

        String result = response.get(10, TimeUnit.SECONDS);

        assertThat(server.takeRequest().getPath()).isEqualTo("/success"); // server received request
        assertThat(result).isEqualTo("success");

        List<SpanData> spans = otelTesting.getSpans();
        assertThat(spans).hasSize(1);

        SpanData span = spans.get(0);

        verifyAttributes(span, url, 200L, responseBody);
    }

    @Test
    public void serverError() throws IOException, InterruptedException {

        String responseBody = "error";
        server.enqueue(new MockResponse().setBody(responseBody).setResponseCode(500));
        server.play();

        URL url = server.getUrl("/error");

        RequestFuture<String> response = RequestFuture.newFuture();
        StringRequest stringRequest =
                new StringRequest(Request.Method.GET, url.toString(), response, response);

        testQueue.addToQueue(stringRequest);

        assertThatThrownBy(() -> response.get(10, TimeUnit.SECONDS))
                .hasCauseInstanceOf(VolleyError.class);

        assertThat(server.takeRequest().getPath()).isEqualTo("/error"); // server received request

        List<SpanData> spans = otelTesting.getSpans();
        assertThat(spans).hasSize(1);

        SpanData span = spans.get(0);

        verifyAttributes(span, url, 500L, responseBody);
    }

    @Test
    public void connectionError() throws IOException {

        server.enqueue(new MockResponse().setBody("should not be received"));
        server.play();

        URL url = new URL("http://" + server.getHostName() + ":" + findUnusedPort() + "/none");

        RequestFuture<String> response = RequestFuture.newFuture();

        StringRequest stringRequest =
                new StringRequest(Request.Method.GET, url.toString(), response, response);
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(50, 0, 1f));

        testQueue.addToQueue(stringRequest);

        // thrown exception type depends on the system, e.g. on MacOS - TimeoutError, on Ubuntu -
        // NoConnectionException
        assertThatThrownBy(() -> response.get(3, TimeUnit.SECONDS)).isInstanceOf(Throwable.class);

        assertThat(server.getRequestCount()).isEqualTo(0); // server received no requests

        List<SpanData> spans = otelTesting.getSpans();
        assertThat(spans).hasSize(1);

        SpanData span = spans.get(0);
        assertThat(span.getName()).isEqualTo("GET");

        assertThat(span.getStatus()).isEqualTo(StatusData.error());

        assertThat(span.getEvents())
                .hasSize(1)
                .allSatisfy(e -> assertThat(e.getName()).isEqualTo("exception"));

        verifyAttributes(span, url, null, null);
    }

    @Test
    public void reusedRequest() throws IOException, InterruptedException {
        String firstResponseBody = "first response";
        String secondResponseBody = "second response";

        server.enqueue(new MockResponse().setBody(firstResponseBody));
        server.enqueue(new MockResponse().setBody(secondResponseBody));
        server.play();

        URL url = server.getUrl("/success");

        TestResponseListener testResponseListener = new TestResponseListener(2);
        StringRequest stringRequest =
                new StringRequest(
                        Request.Method.GET, url.toString(), testResponseListener, error -> {});
        testQueue.addToQueue(stringRequest);
        testQueue.addToQueue(stringRequest);

        testResponseListener.countDownLatch.await(10, TimeUnit.SECONDS);

        assertThat(server.getRequestCount()).isEqualTo(2);

        List<SpanData> spans = otelTesting.getSpans();
        assertThat(spans).hasSize(2);

        SpanData firstSpan = spans.get(0);
        verifyAttributes(firstSpan, url, 200L, firstResponseBody);

        SpanData secondSpan = spans.get(1);
        verifyAttributes(secondSpan, url, 200L, secondResponseBody);
    }

    @Test
    public void concurrency() throws IOException, InterruptedException {
        int count = 50;
        String responseBody = "success";

        for (int i = 0; i < count; i++) {
            server.enqueue(new MockResponse().setBody(responseBody));
        }

        server.play();
        URL url = server.getUrl("/success");

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(4);

        TestResponseListener testResponseListener = new TestResponseListener(count);
        for (int i = 0; i < count; i++) {
            Runnable job =
                    () -> {
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            throw new AssertionError(e);
                        }
                        StringRequest stringRequest =
                                new StringRequest(
                                        Request.Method.GET,
                                        url.toString(),
                                        testResponseListener,
                                        error -> {});
                        testQueue.addToQueue(stringRequest);
                    };
            pool.submit(job);
        }

        latch.countDown();
        testResponseListener.countDownLatch.await(10, TimeUnit.SECONDS);

        assertThat(server.getRequestCount()).isEqualTo(50);

        otelTesting
                .getSpans()
                .forEach(
                        span -> {
                            verifyAttributes(span, url, 200L, "success");
                        });

        pool.shutdown();
    }

    private void verifyAttributes(SpanData span, URL url, Long status, String responseBody) {
        assertThat(span.getName()).isEqualTo("GET");
        assertThat(span.getKind()).isEqualTo(SpanKind.CLIENT);

        Attributes spanAttributes = span.getAttributes();
        assertThat(spanAttributes.get(HttpAttributes.HTTP_REQUEST_METHOD)).isEqualTo("GET");
        assertThat(spanAttributes.get(HttpAttributes.HTTP_RESPONSE_STATUS_CODE)).isEqualTo(status);
        assertThat(spanAttributes.get(UrlAttributes.URL_FULL)).isEqualTo(url.toString());
        assertThat(spanAttributes.get(ServerAttributes.SERVER_PORT)).isEqualTo(url.getPort());
        assertThat(spanAttributes.get(ServerAttributes.SERVER_ADDRESS)).isEqualTo(url.getHost());

        if (responseBody != null) {
            assertThat(span.getAttributes().get(HttpIncubatingAttributes.HTTP_RESPONSE_BODY_SIZE))
                    .isEqualTo(responseBody.length());
        }
    }

    private int findUnusedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            assertThat(socket).isNotNull();
            assertThat(socket.getLocalPort()).isGreaterThan(0);
            return socket.getLocalPort();
        } catch (IOException e) {
            fail("Port is not available");
        }
        return -1;
    }

    private static class TestResponseListener implements Response.Listener<String> {
        private final CountDownLatch countDownLatch;

        TestResponseListener(int count) {
            countDownLatch = new CountDownLatch(count);
        }

        @Override
        public void onResponse(String response) {
            countDownLatch.countDown();
        }
    }
}
