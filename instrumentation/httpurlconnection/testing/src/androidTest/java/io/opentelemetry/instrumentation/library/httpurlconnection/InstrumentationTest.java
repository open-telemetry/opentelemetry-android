/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection;

import static org.junit.Assert.assertEquals;

import io.opentelemetry.android.test.common.OpenTelemetryTestUtils;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InstrumentationTest {
    private static final InMemorySpanExporter inMemorySpanExporter = InMemorySpanExporter.create();

    @Before
    public void setUp() {
        OpenTelemetryTestUtils.setUpSpanExporter(inMemorySpanExporter);
    }

    @After
    public void tearDown() {
        inMemorySpanExporter.reset();
    }

    @Test
    public void testHttpUrlConnectionGetRequest_ShouldBeTraced() {
        HttpUrlConnectionTestUtil.executeGet("http://httpbin.org/get");
        assertEquals(1, inMemorySpanExporter.getFinishedSpanItems().size());
    }

    @Test
    public void testHttpUrlConnectionPostRequest_ShouldBeTraced() {
        HttpUrlConnectionTestUtil.post("http://httpbin.org/post");
        assertEquals(1, inMemorySpanExporter.getFinishedSpanItems().size());
    }

    @Test
    public void
            testHttpUrlConnectionGetRequest_WhenNoStreamFetchedAndNoDisconnectCalled_ShouldNotBeTraced() {
        HttpUrlConnectionTestUtil.executeCustomGet("http://httpbin.org/get", false, false);
        assertEquals(0, inMemorySpanExporter.getFinishedSpanItems().size());
    }

    @Test
    public void
            testHttpUrlConnectionGetRequest_WhenNoStreamFetchedButDisconnectCalled_ShouldBeTraced() {
        HttpUrlConnectionTestUtil.executeCustomGet("http://httpbin.org/get", false, true);
        assertEquals(1, inMemorySpanExporter.getFinishedSpanItems().size());
    }

    @Test
    public void
            testHttpUrlConnectionGetRequest_WhenNoStreamFetchedAndNoDisconnectCalledButHarvesterScheduled_ShouldBeTraced() {
        HttpUrlConnectionTestUtil.executeCustomGet("http://httpbin.org/get", false, false);
        ScheduledExecutorService harvester = scheduleHarvester();
        try {
            Thread.sleep(15000);
            assertEquals(1, inMemorySpanExporter.getFinishedSpanItems().size());
        } catch (InterruptedException e) {
            Assert.fail("Test could not be completed as thread was interrupted while sleeping.");
        } finally {
            harvester.shutdown();
        }
    }

    @Test
    public void
            testHttpUrlConnectionGetRequest_WhenFourConcurrentRequestsAreMade_AllShouldBeTraced() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            executor.submit(() -> HttpUrlConnectionTestUtil.executeGet("http://httpbin.org/get"));
            executor.submit(() -> HttpUrlConnectionTestUtil.executeGet("http://google.com"));
            executor.submit(() -> HttpUrlConnectionTestUtil.executeGet("http://android.com"));
            executor.submit(
                    () -> HttpUrlConnectionTestUtil.executeGet("http://httpbin.org/headers"));

            executor.shutdown();
            // Wait for all tasks to finish execution or timeout
            if (executor.awaitTermination(2, TimeUnit.SECONDS)) {
                // if all tasks finish before timeout
                assertEquals(4, inMemorySpanExporter.getFinishedSpanItems().size());
            } else {
                // if all tasks don't finish before timeout
                Assert.fail(
                        "Test could not be completed as tasks did not complete within the 2s timeout period.");
            }
        } catch (InterruptedException e) {
            // print stack trace to decipher lines that threw InterruptedException as it can be
            // possibly thrown by multiple calls above.
            e.printStackTrace();
            Assert.fail("Test could not be completed due to an interrupted exception.");
        } finally {
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void testHttpUrlConnectionRequest_ContextPropagationHappensAsExpected() {
        Span parentSpan = OpenTelemetryTestUtils.getSpan();

        try (Scope ignored = parentSpan.makeCurrent()) {
            HttpUrlConnectionTestUtil.executeGet("http://httpbin.org/get");
            List<SpanData> spanDataList = inMemorySpanExporter.getFinishedSpanItems();
            if (!spanDataList.isEmpty()) {
                SpanData currentSpanData = spanDataList.get(0);
                assertEquals(
                        parentSpan.getSpanContext().getTraceId(), currentSpanData.getTraceId());
            }
        }

        parentSpan.end();

        assertEquals(2, inMemorySpanExporter.getFinishedSpanItems().size());
    }

    public ScheduledExecutorService scheduleHarvester() {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(
                HttpUrlInstrumentationConfig.getReportIdleConnectionRunnable(),
                0,
                HttpUrlInstrumentationConfig.getReportIdleConnectionInterval(),
                TimeUnit.MILLISECONDS);

        return executorService;
    }
}
