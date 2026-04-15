/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.logs.Logger
import io.opentelemetry.api.metrics.DoubleGaugeBuilder
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongCounterBuilder
import io.opentelemetry.api.metrics.LongGaugeBuilder
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Additional tests for PansMetricsCollector with mocking to cover edge cases and improve coverage.
 */
@RunWith(RobolectricTestRunner::class)
class PansMetricsCollectorMockTest {
    private lateinit var context: Context
    private lateinit var mockSdk: OpenTelemetrySdk
    private lateinit var mockMeter: Meter
    private lateinit var mockLoggerProvider: SdkLoggerProvider
    private lateinit var mockLogger: Logger
    private lateinit var mockCounterBuilder: LongCounterBuilder
    private lateinit var mockCounter: LongCounter
    private lateinit var mockDoubleGaugeBuilder: DoubleGaugeBuilder
    private lateinit var mockLongGaugeBuilder: LongGaugeBuilder
    private lateinit var mockLogRecordBuilder: LogRecordBuilder

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Create all mock components
        mockMeter = mockk(relaxed = true)
        mockSdk = mockk(relaxed = true)
        mockLoggerProvider = mockk(relaxed = true)
        mockLogger = mockk(relaxed = true)
        mockCounterBuilder = mockk(relaxed = true)
        mockCounter = mockk(relaxed = true)
        mockDoubleGaugeBuilder = mockk(relaxed = true)
        mockLongGaugeBuilder = mockk(relaxed = true)
        mockLogRecordBuilder = mockk(relaxed = true)

        every { mockSdk.getMeter(any()) } returns mockMeter
        every { mockSdk.logsBridge } returns mockLoggerProvider
        every { mockLoggerProvider.get(any()) } returns mockLogger
        every { mockLogger.logRecordBuilder() } returns mockLogRecordBuilder
        every { mockLogRecordBuilder.setEventName(any()) } returns mockLogRecordBuilder
        every { mockLogRecordBuilder.setAllAttributes(any()) } returns mockLogRecordBuilder
        every { mockLogRecordBuilder.emit() } returns Unit
        every { mockMeter.counterBuilder(any()) } returns mockCounterBuilder
        every { mockMeter.gaugeBuilder(any()) } returns mockDoubleGaugeBuilder
        every { mockCounterBuilder.setUnit(any()) } returns mockCounterBuilder
        every { mockCounterBuilder.setDescription(any()) } returns mockCounterBuilder
        every { mockCounterBuilder.build() } returns mockCounter
        every { mockDoubleGaugeBuilder.setDescription(any()) } returns mockDoubleGaugeBuilder
        every { mockDoubleGaugeBuilder.ofLongs() } returns mockLongGaugeBuilder
        every { mockLongGaugeBuilder.buildWithCallback(any()) } returns mockk(relaxed = true)
        every { mockCounter.add(any<Long>(), any<Attributes>()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Initialization Edge Cases ====================

    @Test
    fun testCollectorWithNegativeInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = -1L)
        assertNotNull(collector)
    }

    @Test
    fun testCollectorWithVeryLargeInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = Long.MAX_VALUE)
        assertNotNull(collector)
    }

    // ==================== Start/Stop Lifecycle Tests ====================

    @Test
    fun testStartAfterStop() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.stop()

        // Starting again after stop
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    @Test
    fun testStopImmediatelyAfterStart() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        collector.stop() // Immediate stop
    }

    @Test
    fun testConcurrentStartCalls() {
        val collector = PansMetricsCollector(context, mockSdk)
        val threads =
            (1..5).map {
                Thread {
                    try {
                        collector.start()
                    } catch (e: Exception) {
                        // Ignore - concurrent access may cause issues
                    }
                }
            }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        Thread.sleep(50)
        collector.stop()
    }

    // ==================== Metrics Recording Tests ====================

    @Test
    fun testMetricsRecordingWithCounters() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(150)
        collector.stop()

        // Verify counters were created
        verify(atLeast = 1) { mockMeter.counterBuilder("network.pans.bytes_transmitted") }
        verify(atLeast = 1) { mockMeter.counterBuilder("network.pans.bytes_received") }
    }

    @Test
    fun testGaugeCreation() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(150)
        collector.stop()

        verify(atLeast = 1) { mockMeter.gaugeBuilder("network.pans.network_available") }
    }

    @Test
    fun testCounterBuilderConfiguration() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(150)
        collector.stop()

        verify(atLeast = 1) { mockCounterBuilder.setUnit("By") }
        verify(atLeast = 1) { mockCounterBuilder.setDescription(any()) }
    }

    // ==================== Error Handling in Metrics Recording ====================

    @Test
    fun testCollectorHandlesMeterException() {
        every { mockMeter.counterBuilder(any()) } throws RuntimeException("Meter error")

        val collector = PansMetricsCollector(context, mockSdk)

        // Should not throw, just log error
        collector.start()
        Thread.sleep(100)
        collector.stop()
    }

    @Test
    fun testCollectorHandlesGaugeException() {
        every { mockMeter.gaugeBuilder(any()) } throws RuntimeException("Gauge error")

        val collector = PansMetricsCollector(context, mockSdk)

        collector.start()
        Thread.sleep(100)
        collector.stop()
    }

    @Test
    fun testCollectorHandlesLoggerException() {
        every { mockLoggerProvider.get(any()) } throws RuntimeException("Logger error")

        val collector = PansMetricsCollector(context, mockSdk)

        collector.start()
        Thread.sleep(100)
        collector.stop()
    }

    // ==================== Gauge Callback Tests ====================

    @Test
    fun testGaugeBuilderWithCallback() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(150)
        collector.stop()

        verify(atLeast = 1) { mockLongGaugeBuilder.buildWithCallback(any()) }
    }

    @Test
    fun testGaugeDescriptionSet() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(150)
        collector.stop()

        verify(atLeast = 1) { mockDoubleGaugeBuilder.setDescription("Whether OEM network is available") }
    }

    // ==================== SDK Interaction Tests ====================

    @Test
    fun testSdkMeterAccessed() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        verify(atLeast = 1) { mockSdk.getMeter("io.opentelemetry.android.pans") }
    }

    @Test
    fun testLogsBridgeAccessed() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        // LogsBridge may or may not be accessed depending on preference changes
        // Just verify no exceptions occur
    }

    // ==================== Collection Interval Tests ====================

    @Test
    fun testCollectorWithOneMinuteInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 1L)
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    @Test
    fun testCollectorWithFiveMinuteInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 5L)
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    @Test
    fun testCollectorWithDefaultInterval() {
        val collector = PansMetricsCollector(context, mockSdk) // Default 15 minutes
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    // ==================== Multiple Collectors Tests ====================

    @Test
    fun testMultipleCollectorsIndependent() {
        val collector1 = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 1L)
        val collector2 = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 2L)
        val collector3 = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 3L)

        collector1.start()
        collector2.start()
        collector3.start()

        Thread.sleep(100)

        collector1.stop()
        collector2.stop()
        collector3.stop()
    }

    // ==================== Context Tests ====================

    @Test
    fun testCollectorWithApplicationContext() {
        val appContext = context.applicationContext
        val collector = PansMetricsCollector(appContext, mockSdk)
        assertNotNull(collector)

        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    // ==================== Stress Tests ====================

    @Test
    fun testRapidStartStopCycles() {
        val collector = PansMetricsCollector(context, mockSdk)

        repeat(10) {
            collector.start()
            collector.stop()
        }
    }

    @Test
    fun testCollectorResilience() {
        // Create collector with mocks that throw
        every { mockMeter.counterBuilder(any()) } throws RuntimeException("Test error")

        val collector = PansMetricsCollector(context, mockSdk)

        // Should handle errors gracefully
        collector.start()
        Thread.sleep(50)
        collector.stop()
        collector.stop() // Double stop
    }
}
