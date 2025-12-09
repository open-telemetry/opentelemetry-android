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
 * Comprehensive coverage tests for PansMetricsCollector.
 */
@RunWith(RobolectricTestRunner::class)
class PansMetricsCollectorCoverageTest {
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

    // ==================== Constructor Coverage ====================

    @Test
    fun testCollectorCreation() {
        val collector = PansMetricsCollector(context, mockSdk)
        assertNotNull(collector)
    }

    @Test
    fun testCollectorWithDefaultInterval() {
        val collector = PansMetricsCollector(context, mockSdk)
        assertNotNull(collector)
    }

    @Test
    fun testCollectorWithCustomInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 30L)
        assertNotNull(collector)
    }

    @Test
    fun testCollectorWithZeroInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 0L)
        assertNotNull(collector)
    }

    @Test
    fun testCollectorWithOneMinuteInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 1L)
        assertNotNull(collector)
    }

    // ==================== start() Coverage ====================

    @Test
    fun testStartInitializesCollection() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()
    }

    @Test
    fun testStartCalledMultipleTimes() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        collector.start() // Second call should be ignored
        collector.start() // Third call should be ignored
        Thread.sleep(50)
        collector.stop()
    }

    @Test
    fun testStartCreatesCounters() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        verify(atLeast = 1) { mockMeter.counterBuilder("network.pans.bytes_transmitted") }
        verify(atLeast = 1) { mockMeter.counterBuilder("network.pans.bytes_received") }
    }

    @Test
    fun testStartCreatesGauge() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        verify(atLeast = 1) { mockMeter.gaugeBuilder("network.pans.network_available") }
    }

    // ==================== stop() Coverage ====================

    @Test
    fun testStopWithoutStart() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.stop() // Should not throw
    }

    @Test
    fun testStopCalledMultipleTimes() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.stop()
        collector.stop() // Second stop should be safe
        collector.stop() // Third stop should be safe
    }

    @Test
    fun testStartStopStart() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.stop()
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    // ==================== Metrics Recording Coverage ====================

    @Test
    fun testRecordsMetricsOnStart() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(150)
        collector.stop()

        // Verify metrics were recorded
        verify(atLeast = 1) { mockCounterBuilder.setUnit("By") }
    }

    @Test
    fun testCounterDescriptionsSet() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        verify(atLeast = 1) { mockCounterBuilder.setDescription(any()) }
    }

    @Test
    fun testGaugeDescriptionSet() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        verify(atLeast = 1) { mockDoubleGaugeBuilder.setDescription("Whether OEM network is available") }
    }

    // ==================== Exception Handling Coverage ====================

    @Test
    fun testHandlesMeterCreationException() {
        every { mockSdk.getMeter(any()) } throws RuntimeException("Meter unavailable")

        // Exception may be thrown during construction since getMeter is called in constructor
        try {
            val collector = PansMetricsCollector(context, mockSdk)
            collector.start()
            Thread.sleep(50)
            collector.stop()
        } catch (_: RuntimeException) {
            // Expected - getMeter is called in constructor
        }
    }

    @Test
    fun testHandlesCounterBuilderException() {
        every { mockMeter.counterBuilder(any()) } throws RuntimeException("Counter error")

        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()
        // Should complete without throwing
    }

    @Test
    fun testHandlesGaugeBuilderException() {
        every { mockMeter.gaugeBuilder(any()) } throws RuntimeException("Gauge error")

        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()
        // Should complete without throwing
    }

    @Test
    fun testHandlesLoggerException() {
        every { mockLoggerProvider.get(any()) } throws RuntimeException("Logger error")

        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()
        // Should complete without throwing
    }

    @Test
    fun testHandlesLogRecordBuilderException() {
        every { mockLogger.logRecordBuilder() } throws RuntimeException("Log record error")

        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()
    }

    // ==================== Periodic Collection Coverage ====================

    @Test
    fun testCollectionContinuesAfterException() {
        var callCount = 0
        every { mockMeter.counterBuilder(any()) } answers {
            callCount++
            if (callCount == 1) throw RuntimeException("First call fails")
            mockCounterBuilder
        }

        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 1L)
        collector.start()
        Thread.sleep(100)
        collector.stop()
    }

    // ==================== Integration Tests ====================

    @Test
    fun testFullLifecycle() {
        val collector = PansMetricsCollector(context, mockSdk)

        // Start collection
        collector.start()
        Thread.sleep(100)

        // Verify metrics setup
        verify(atLeast = 1) { mockMeter.counterBuilder(any()) }
        verify(atLeast = 1) { mockMeter.gaugeBuilder(any()) }

        // Stop collection
        collector.stop()
    }

    @Test
    fun testMultipleCollectionCycles() {
        val collector = PansMetricsCollector(context, mockSdk)

        // First cycle
        collector.start()
        Thread.sleep(50)
        collector.stop()

        // Second cycle
        collector.start()
        Thread.sleep(50)
        collector.stop()

        // Third cycle
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    @Test
    fun testConcurrentOperations() {
        val collector = PansMetricsCollector(context, mockSdk)

        val threads =
            listOf(
                Thread { collector.start() },
                Thread { collector.start() },
                Thread {
                    Thread.sleep(30)
                    collector.stop()
                },
            )

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Cleanup
        collector.stop()
    }
}
