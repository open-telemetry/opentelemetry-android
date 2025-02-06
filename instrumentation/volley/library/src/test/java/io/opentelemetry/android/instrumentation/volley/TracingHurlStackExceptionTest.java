/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ExceptionAttributes.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.android.volley.Request;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TracingHurlStackExceptionTest {

    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private TestRequestQueue testQueue;
    private StuckTestHelper stuckTestHelper;

    @Before
    public void setup() {
        // setup Volley with TracingHurlStack
        HurlStack tracingHurlStack =
                VolleyTracing.create(otelTesting.getOpenTelemetry())
                        .newHurlStack(new FailingURLRewriter());
        testQueue = TestRequestQueue.create(tracingHurlStack);
        stuckTestHelper = StuckTestHelper.start();
    }

    @After
    public void cleanup() {
        stuckTestHelper.close();
    }

    @Test
    public void spanDecoration_error() {
        RequestFuture<String> response = RequestFuture.newFuture();
        StringRequest stringRequest =
                new StringRequest(Request.Method.GET, "whatever", response, response);

        testQueue.addToQueue(stringRequest);

        assertThatThrownBy(() -> response.get(3, TimeUnit.SECONDS))
                .hasRootCauseInstanceOf(RuntimeException.class);

        List<SpanData> spans = otelTesting.getSpans();
        assertThat(spans).hasSize(1);

        SpanData span = spans.get(0);
        assertThat(span)
                .hasEventsSatisfyingExactly(
                        e ->
                                e.hasName("exception")
                                        .hasAttributesSatisfying(
                                                a ->
                                                        assertThat(a)
                                                                .containsEntry(
                                                                        EXCEPTION_TYPE,
                                                                        "java.lang.RuntimeException")
                                                                .containsEntry(
                                                                        EXCEPTION_MESSAGE,
                                                                        "Something went wrong")
                                                                .containsKey(
                                                                        EXCEPTION_STACKTRACE)));
    }

    static class FailingURLRewriter implements HurlStack.UrlRewriter {

        @Override
        public String rewriteUrl(String originalUrl) {
            throw new RuntimeException("Something went wrong");
        }
    }
}
