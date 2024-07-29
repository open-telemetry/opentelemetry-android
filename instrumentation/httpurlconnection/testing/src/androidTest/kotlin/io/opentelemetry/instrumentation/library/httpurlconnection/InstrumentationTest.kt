/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import io.opentelemetry.android.test.common.OpenTelemetryTestUtils
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlConnectionTestUtil.executeGet
import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlConnectionTestUtil.post
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlConnectionSingletons
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class InstrumentationTest {
    private lateinit var inMemorySpanExporter: InMemorySpanExporter
    private lateinit var openTelemetryTestUtils: OpenTelemetryTestUtils

    @Before
    fun setUp() {
        inMemorySpanExporter = InMemorySpanExporter.create()
        openTelemetryTestUtils = OpenTelemetryTestUtils()
        HttpUrlConnectionSingletons.setThreadLocalInstrumenterForTesting(openTelemetryTestUtils.setUpSpanExporter(inMemorySpanExporter))
    }

    @After
    fun tearDown() {
        inMemorySpanExporter.shutdown()
        HttpUrlConnectionSingletons.removeThreadLocalInstrumenterForTesting()
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
            if (executor.awaitTermination(2, TimeUnit.SECONDS)) {
                // if all tasks finish before timeout
                assertThat(inMemorySpanExporter.finishedSpanItems.size).isEqualTo(4)
            } else {
                // if all tasks don't finish before timeout
                fail(
                    "Test could not be completed as tasks did not complete within the 2s timeout period.",
                )
            }
        } catch (e: InterruptedException) {
            // print stack trace to decipher lines that threw InterruptedException as it can be
            // possibly thrown by multiple calls above.
            e.printStackTrace()
            fail("Test could not be completed due to an interrupted exception.")
        } finally {
            if (!executor.isShutdown) {
                executor.shutdownNow()
            }
        }
    }

    @Test
    fun testHttpUrlConnectionRequest_ContextPropagationHappensAsExpected() {
        val parentSpan = openTelemetryTestUtils.getSpan()

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
}
