/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader.Companion.getInstrumentation
import io.opentelemetry.android.test.common.OpenTelemetryRumRule
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlConnectionTestUtil.executeGet
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlConnectionTestUtil.post
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InstrumentationTest {
    @JvmField
    @Rule
    var openTelemetryRumRule: OpenTelemetryRumRule = OpenTelemetryRumRule()

    @Test
    fun testHttpUrlConnectionGetRequest_ShouldBeTraced() {
        executeGet("http://httpbin.org/get")
        assertThat(openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size).isEqualTo(1)
    }

    @Test
    fun testHttpUrlConnectionPostRequest_ShouldBeTraced() {
        post("http://httpbin.org/post")
        assertThat(openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size).isEqualTo(1)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenNoStreamFetchedAndNoDisconnectCalled_ShouldNotBeTraced() {
        executeGet("http://httpbin.org/get", false, false)
        assertThat(openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size).isEqualTo(0)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenNoStreamFetchedButDisconnectCalled_ShouldBeTraced() {
        executeGet("http://httpbin.org/get", false)
        assertThat(openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size).isEqualTo(1)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenFourConcurrentRequestsAreMade_AllShouldBeTraced() {
        val executor = Executors.newFixedThreadPool(4)
        val latch = CountDownLatch(4)
        try {
            executor.submit { executeGet("http://httpbin.org/get") { latch.countDown() } }
            executor.submit { executeGet("http://google.com") { latch.countDown() } }
            executor.submit { executeGet("http://android.com") { latch.countDown() } }
            executor.submit { executeGet("http://httpbin.org/headers") { latch.countDown() } }

            executor.shutdown()

            // Timeout large enough to allow tests to pass consistently in CI, but happy path is fast.
            // Only in failure will this be slow. As soon as all 4 executor jobs finish, this completes.
            assertThat(latch.await(1, TimeUnit.MINUTES)).isTrue()
            assertThat(openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size).isEqualTo(4)
        } finally {
            if (!executor.isShutdown) {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun testHttpUrlConnectionRequest_ContextPropagationHappensAsExpected() {
        val parentSpan = openTelemetryRumRule.getSpan()

        parentSpan.makeCurrent().use {
            executeGet("http://httpbin.org/get")
            val spanDataList = openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems
            if (spanDataList.isNotEmpty()) {
                val currentSpanData = spanDataList[0]
                assertThat(currentSpanData.traceId).isEqualTo(parentSpan.spanContext.traceId)
            }
        }
        parentSpan.end()

        assertThat(openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size).isEqualTo(2)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenNoStreamFetchedAndNoDisconnectCalledButHarvesterScheduled_ShouldBeTraced() {
        executeGet("http://httpbin.org/get", false, false)

        // no span created without harvester thread
        assertThat(openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size).isEqualTo(0)

        val instrumentation =
            getInstrumentation(
                HttpUrlInstrumentation::class.java,
            )
        // setting a -1ms connection inactivity timeout for testing to ensure harvester sees it as 1ms elapsed
        // and we don't have to include any wait timers in the test. 0ms does not work as the time difference
        // between last connection activity and harvester time elapsed check is much lesser than 1ms due to
        // our high speed modern CPUs.
        instrumentation?.setConnectionInactivityTimeoutMsForTesting(-1)
        // Running the harvester runnable once instead of scheduling it to run periodically,
        // so we can synchronously assert instead of waiting for another threads execution to finish
        instrumentation?.reportIdleConnectionRunnable?.run()

        // span created with harvester thread
        assertThat(openTelemetryRumRule.inMemorySpanExporter.finishedSpanItems.size).isEqualTo(1)
    }

    companion object {
        private const val TAG = "HttpURLInstrumentedTest"
    }
}
