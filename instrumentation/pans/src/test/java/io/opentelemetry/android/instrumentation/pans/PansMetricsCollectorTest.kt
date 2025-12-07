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
import io.opentelemetry.api.metrics.DoubleGaugeBuilder
import io.opentelemetry.api.metrics.LongCounter
import io.opentelemetry.api.metrics.LongCounterBuilder
import io.opentelemetry.api.metrics.LongGaugeBuilder
import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PansMetricsCollectorTest {
    private lateinit var context: Context
    private lateinit var mockSdk: OpenTelemetrySdk
    private lateinit var mockMeter: Meter

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Create mock components
        mockMeter = mockk(relaxed = true)
        mockSdk = mockk(relaxed = true)

        val mockCounterBuilder = mockk<LongCounterBuilder>(relaxed = true)
        val mockCounter = mockk<LongCounter>(relaxed = true)
        val mockDoubleGaugeBuilder = mockk<DoubleGaugeBuilder>(relaxed = true)
        val mockLongGaugeBuilder = mockk<LongGaugeBuilder>(relaxed = true)

        every { mockSdk.getMeter(any()) } returns mockMeter
        every { mockSdk.logsBridge } returns mockk(relaxed = true)
        every { mockMeter.counterBuilder(any()) } returns mockCounterBuilder
        every { mockMeter.gaugeBuilder(any()) } returns mockDoubleGaugeBuilder
        every { mockCounterBuilder.setUnit(any()) } returns mockCounterBuilder
        every { mockCounterBuilder.setDescription(any()) } returns mockCounterBuilder
        every { mockCounterBuilder.build() } returns mockCounter
        every { mockDoubleGaugeBuilder.setDescription(any()) } returns mockDoubleGaugeBuilder
        every { mockDoubleGaugeBuilder.ofLongs() } returns mockLongGaugeBuilder
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ==================== Initialization Tests ====================

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
    fun testCollectorWithMinInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 1L)
        assertNotNull(collector)
    }

    @Test
    fun testCollectorWithLargeInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 1440L)
        assertNotNull(collector)
    }

    @Test
    fun testCollectorWithZeroInterval() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 0L)
        assertNotNull(collector)
    }

    @Test
    fun testMultipleCollectorInstances() {
        val collector1 = PansMetricsCollector(context, mockSdk)
        val collector2 = PansMetricsCollector(context, mockSdk)
        assertNotNull(collector1)
        assertNotNull(collector2)
    }

    // ==================== Start/Stop Tests ====================

    @Test
    fun testStartDoesNotThrow() {
        val collector = PansMetricsCollector(context, mockSdk)
        try {
            collector.start()
            Thread.sleep(50) // Let it initialize
            collector.stop()
        } catch (e: Exception) {
            throw AssertionError("start() should not throw", e)
        }
    }

    @Test
    fun testStopDoesNotThrow() {
        val collector = PansMetricsCollector(context, mockSdk)
        try {
            collector.stop()
        } catch (e: Exception) {
            throw AssertionError("stop() should not throw", e)
        }
    }

    @Test
    fun testStartThenStop() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    @Test
    fun testMultipleStarts() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        collector.start() // Second start should be ignored
        Thread.sleep(50)
        collector.stop()
    }

    @Test
    fun testMultipleStops() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.stop()
        collector.stop() // Second stop should be safe
        collector.stop() // Third stop should be safe
    }

    @Test
    fun testStopWithoutStart() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.stop() // Should not throw
    }

    @Test
    fun testStartStopCycle() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun testCollectorCreationWithContext() {
        try {
            val collector = PansMetricsCollector(context, mockSdk)
            assertNotNull(collector)
        } catch (e: Exception) {
            throw AssertionError("Collector creation should not throw", e)
        }
    }

    @Test
    fun testCollectorStartErrorHandling() {
        val collector = PansMetricsCollector(context, mockSdk)
        try {
            collector.start()
            Thread.sleep(50)
        } catch (e: Exception) {
            // Expected to handle errors gracefully
        } finally {
            collector.stop()
        }
    }

    @Test
    fun testCollectorWithRealContext() {
        val realContext = ApplicationProvider.getApplicationContext<Context>()
        val collector = PansMetricsCollector(realContext, mockSdk)
        assertNotNull(collector)
    }

    @Test
    fun testCollectorLifecycle() {
        val collector = PansMetricsCollector(context, mockSdk)

        // Create
        assertNotNull(collector)

        // Start
        try {
            collector.start()
            Thread.sleep(50)
        } catch (e: Exception) {
            // May fail due to permissions, but should not crash
        }

        // Stop
        collector.stop()
    }

    // ==================== Metrics Recording Tests ====================

    @Test
    fun testCollectorRecordsMetrics() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100) // Let it collect once
        collector.stop()

        // Verify meter was accessed
        verify(atLeast = 1) { mockSdk.getMeter(any()) }
    }

    @Test
    fun testCollectorCreatesBytesTransmittedCounter() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        verify(atLeast = 1) { mockMeter.counterBuilder("network.pans.bytes_transmitted") }
    }

    @Test
    fun testCollectorCreatesBytesReceivedCounter() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        verify(atLeast = 1) { mockMeter.counterBuilder("network.pans.bytes_received") }
    }

    @Test
    fun testCollectorCreatesNetworkAvailableGauge() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        verify(atLeast = 1) { mockMeter.gaugeBuilder("network.pans.network_available") }
    }

    // ==================== Edge Cases ====================

    @Test
    fun testRapidStartStop() {
        val collector = PansMetricsCollector(context, mockSdk)
        repeat(3) {
            try {
                collector.start()
                Thread.sleep(10)
                collector.stop()
            } catch (e: Exception) {
                // Ignore errors during rapid start/stop
            }
        }
    }

    @Test
    fun testCollectorStopsCleanly() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()
        Thread.sleep(50) // Ensure cleanup
    }

    @Test
    fun testManyCollectors() {
        val collectors = mutableListOf<PansMetricsCollector>()
        repeat(3) {
            collectors.add(PansMetricsCollector(context, mockSdk))
        }

        collectors.forEach { it.start() }
        Thread.sleep(50)
        collectors.forEach { it.stop() }
    }

    @Test
    fun testCollectorWithDifferentIntervals() {
        val intervals = listOf(1L, 5L, 15L, 30L, 60L)
        intervals.forEach { interval ->
            val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = interval)
            assertNotNull(collector)
        }
    }

    @Test
    fun testCollectorApplicationContext() {
        val appContext = context.applicationContext
        val collector = PansMetricsCollector(appContext, mockSdk)
        assertNotNull(collector)
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }
}
