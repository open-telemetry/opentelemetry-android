/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import io.opentelemetry.android.test.common.OpenTelemetryTestUtils
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlConnectionTestUtil.executeGet
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlConnectionTestUtil.post
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InstrumentationTest {
    companion object {
        private val inMemorySpanExporter: InMemorySpanExporter = InMemorySpanExporter.create()

        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            OpenTelemetryTestUtils.setUpSpanExporter(inMemorySpanExporter)
        }
    }

    @After
    fun tearDown() {
        inMemorySpanExporter.reset()
    }

    @Test
    fun testHttpUrlConnectionGetRequest_ShouldBeTraced() {
        executeGet("http://httpbin.org/get")
        assertThat(inMemorySpanExporter.finishedSpanItems.size).isEqualTo(1)
    }

    @Test
    fun testHttpUrlConnectionPostRequest_ShouldBeTraced() {
        post("http://httpbin.org/post")
        assertThat(inMemorySpanExporter.finishedSpanItems.size).isEqualTo(1)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenNoStreamFetchedAndNoDisconnectCalled_ShouldNotBeTraced() {
        executeGet("http://httpbin.org/get", false, false)
        assertThat(inMemorySpanExporter.finishedSpanItems.size).isEqualTo(0)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenNoStreamFetchedButDisconnectCalled_ShouldBeTraced() {
        executeGet("http://httpbin.org/get", false)
        assertThat(inMemorySpanExporter.finishedSpanItems.size).isEqualTo(1)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenFourConcurrentRequestsAreMade_AllShouldBeTraced() {
        val executor = Executors.newFixedThreadPool(4)
        try {
            executor.submit { executeGet("http://httpbin.org/get") }
            executor.submit { executeGet("http://google.com") }
            executor.submit { executeGet("http://android.com") }
            executor.submit { executeGet("http://httpbin.org/headers") }

            executor.shutdown()

            // Wait for all tasks to finish execution or timeout
            assertThat(executor.awaitTermination(2, TimeUnit.SECONDS))
                .withFailMessage("Test could not be completed as tasks did not complete within the 2s timeout period.")
                .isTrue()

            assertThat(inMemorySpanExporter.finishedSpanItems.size).isEqualTo(4)
        } finally {
            if (!executor.isShutdown) {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun testHttpUrlConnectionRequest_ContextPropagationHappensAsExpected() {
        val parentSpan = OpenTelemetryTestUtils.getSpan()

        parentSpan.makeCurrent().use {
            executeGet("http://httpbin.org/get")
            val spanDataList = inMemorySpanExporter.finishedSpanItems
            if (spanDataList.isNotEmpty()) {
                val currentSpanData = spanDataList[0]
                assertThat(currentSpanData.traceId).isEqualTo(parentSpan.spanContext.traceId)
            }
        }
        parentSpan.end()

        assertThat(inMemorySpanExporter.finishedSpanItems.size).isEqualTo(2)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenNoStreamFetchedAndNoDisconnectCalledButHarvesterScheduled_ShouldBeTraced() {
        executeGet("http://httpbin.org/get", false, false)
        // setting a -1ms connection inactivity timeout for testing to ensure harvester sees it as 1ms elapsed
        // and we don't have to include any wait timers in the test. 0ms does not work as the time difference
        // between last connection activity and harvester time elapsed check is much lesser than 1ms due to
        // our high speed modern CPUs.
        HttpUrlInstrumentationConfig.setConnectionInactivityTimeoutMsForTesting(-1)
        // Running the harvester runnable once instead of scheduling it to run periodically,
        // so we can synchronously assert instead of waiting for another threads execution to finish
        HttpUrlInstrumentationConfig.getReportIdleConnectionRunnable().run()
        assertThat(inMemorySpanExporter.finishedSpanItems.size).isEqualTo(1)
    }
}
