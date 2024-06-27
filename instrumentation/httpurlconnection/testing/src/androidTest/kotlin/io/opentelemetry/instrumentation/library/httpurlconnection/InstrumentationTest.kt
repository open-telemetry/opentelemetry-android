/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import io.opentelemetry.android.test.common.OpenTelemetryTestUtils
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlConnectionTestUtil.executeGet
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlConnectionTestUtil.post
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class InstrumentationTest {
    @Before
    fun setUp() {
        OpenTelemetryTestUtils.setUpSpanExporter(inMemorySpanExporter)
    }

    @After
    fun tearDown() {
        inMemorySpanExporter.reset()
    }

    @Test
    fun testHttpUrlConnectionGetRequest_ShouldBeTraced() {
        executeGet("http://httpbin.org/get")
        Assert.assertEquals(1, inMemorySpanExporter.finishedSpanItems.size)
    }

    @Test
    fun testHttpUrlConnectionPostRequest_ShouldBeTraced() {
        post("http://httpbin.org/post")
        Assert.assertEquals(1, inMemorySpanExporter.finishedSpanItems.size)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenNoStreamFetchedAndNoDisconnectCalled_ShouldNotBeTraced() {
        executeGet("http://httpbin.org/get", false, false)
        Assert.assertEquals(0, inMemorySpanExporter.finishedSpanItems.size)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenNoStreamFetchedButDisconnectCalled_ShouldBeTraced() {
        executeGet("http://httpbin.org/get", false)
        Assert.assertEquals(1, inMemorySpanExporter.finishedSpanItems.size)
    }

    @Test
    fun testHttpUrlConnectionGetRequest_WhenNoStreamFetchedAndNoDisconnectCalledButHarvesterScheduled_ShouldBeTraced() {
        executeGet("http://httpbin.org/get", false, false)
        val harvester = scheduleHarvester()
        try {
            Thread.sleep(15000)
            Assert.assertEquals(1, inMemorySpanExporter.finishedSpanItems.size)
        } catch (e: InterruptedException) {
            Assert.fail("Test could not be completed as thread was interrupted while sleeping.")
        } finally {
            harvester.shutdown()
        }
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
            if (executor.awaitTermination(2, TimeUnit.SECONDS)) {
                // if all tasks finish before timeout
                Assert.assertEquals(4, inMemorySpanExporter.finishedSpanItems.size)
            } else {
                // if all tasks don't finish before timeout
                Assert.fail(
                    "Test could not be completed as tasks did not complete within the 2s timeout period.",
                )
            }
        } catch (e: InterruptedException) {
            // print stack trace to decipher lines that threw InterruptedException as it can be
            // possibly thrown by multiple calls above.
            e.printStackTrace()
            Assert.fail("Test could not be completed due to an interrupted exception.")
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
                Assert.assertEquals(
                    parentSpan.spanContext.traceId,
                    currentSpanData.traceId,
                )
            }
        }
        parentSpan.end()

        Assert.assertEquals(2, inMemorySpanExporter.finishedSpanItems.size)
    }

    private fun scheduleHarvester(): ScheduledExecutorService {
        val executorService = Executors.newSingleThreadScheduledExecutor()
        executorService.scheduleWithFixedDelay(
            HttpUrlInstrumentationConfig.getReportIdleConnectionRunnable(),
            0,
            HttpUrlInstrumentationConfig.getReportIdleConnectionInterval(),
            TimeUnit.MILLISECONDS,
        )

        return executorService
    }

    companion object {
        private val inMemorySpanExporter: InMemorySpanExporter = InMemorySpanExporter.create()
    }
}
