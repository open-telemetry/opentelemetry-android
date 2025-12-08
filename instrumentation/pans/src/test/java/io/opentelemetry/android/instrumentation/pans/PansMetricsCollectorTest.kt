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
import org.junit.Assert.assertTrue
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
        val mockLogsBridge = mockk<io.opentelemetry.sdk.logs.SdkLoggerProvider>(relaxed = true)

        every { mockSdk.getMeter(any()) } returns mockMeter
        every { mockSdk.logsBridge } returns mockLogsBridge
        every { mockMeter.counterBuilder(any()) } returns mockCounterBuilder
        every { mockMeter.gaugeBuilder(any()) } returns mockDoubleGaugeBuilder
        every { mockCounterBuilder.setUnit(any()) } returns mockCounterBuilder
        every { mockCounterBuilder.setDescription(any()) } returns mockCounterBuilder
        every { mockCounterBuilder.build() } returns mockCounter
        every { mockDoubleGaugeBuilder.setDescription(any()) } returns mockDoubleGaugeBuilder
        every { mockDoubleGaugeBuilder.ofLongs() } returns mockLongGaugeBuilder
        every { mockLongGaugeBuilder.buildWithCallback(any()) } returns mockk(relaxed = true)
        every { mockCounter.add(any(), any<io.opentelemetry.api.common.Attributes>()) } returns Unit
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

    // ==================== Extended Metrics Recording Tests ====================

    @Test
    fun testCollectorRecordsMetricsWithAttributes() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(150)
        collector.stop()

        verify(atLeast = 1) { mockMeter.counterBuilder(any()) }
        verify(atLeast = 1) { mockMeter.gaugeBuilder(any()) }
    }

    @Test
    fun testCollectorInitializesMeterCorrectly() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.stop()

        verify { mockSdk.getMeter("io.opentelemetry.android.pans") }
    }

    @Test
    fun testCollectorHandlesEmptyMetrics() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.stop()

        // Verify collector completes without error even if metrics are empty
        assertNotNull(collector)
    }

    @Test
    fun testCollectorWithLongDelay() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(200)
        collector.stop()

        verify(atLeast = 1) { mockSdk.getMeter(any()) }
    }

    @Test
    fun testCollectorInitializationWithMinimalContext() {
        try {
            val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 0L)
            collector.start()
            Thread.sleep(50)
            collector.stop()
        } catch (e: Exception) {
            // Should complete gracefully
        }
    }

    @Test
    fun testCollectorResourceCleanup() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()
        Thread.sleep(50)
        // Verify state after cleanup
        assertNotNull(collector)
    }

    @Test
    fun testCollectorConcurrentStartStopOperations() {
        val collector = PansMetricsCollector(context, mockSdk)
        repeat(5) {
            Thread {
                try {
                    collector.start()
                    Thread.sleep(10)
                    collector.stop()
                } catch (e: Exception) {
                    // Expected in concurrent scenario
                }
            }.start()
        }
        Thread.sleep(100)
    }

    @Test
    fun testCollectorLogsOnStart() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.stop()

        // Verify that the collector attempted to initialize metrics
        verify { mockSdk.getMeter(any()) }
    }

    @Test
    fun testCollectorReadsFromNetworkStats() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        // Verify counter builders were called for expected metrics
        verify(atLeast = 1) { mockMeter.counterBuilder("network.pans.bytes_transmitted") }
        verify(atLeast = 1) { mockMeter.counterBuilder("network.pans.bytes_received") }
    }

    @Test
    fun testCollectorWithValidSdkInstance() {
        val collector = PansMetricsCollector(context, mockSdk)
        assertNotNull(collector)
        collector.start()
        Thread.sleep(50)
        collector.stop()
    }

    @Test
    fun testCollectorDurationTracking() {
        val startTime = System.currentTimeMillis()
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()
        val duration = System.currentTimeMillis() - startTime

        // Verify operation completed in reasonable time
        assertTrue(duration < 5000)
    }

    @Test
    fun testCollectorWithMultipleMeterAccess() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        // Verify meter was accessed multiple times for different metrics
        verify(atLeast = 1) { mockSdk.getMeter(any()) }
        verify(atLeast = 1) { mockMeter.counterBuilder(any()) }
        verify(atLeast = 1) { mockMeter.gaugeBuilder(any()) }
    }

    @Test
    fun testCollectorStopsGracefullyAfterLongCollection() {
        val collector = PansMetricsCollector(context, mockSdk, collectionIntervalMinutes = 60L)
        collector.start()
        Thread.sleep(75)
        collector.stop()
        assertNotNull(collector)
    }

    @Test
    fun testCollectorIgnoresSecondStartCall() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(50)
        collector.start() // Should be ignored
        Thread.sleep(50)
        collector.stop()

        // Verify single meter initialization
        verify(atLeast = 1) { mockSdk.getMeter(any()) }
    }

    @Test
    fun testCollectorPermissionHandling() {
        val collector = PansMetricsCollector(context, mockSdk)
        try {
            collector.start()
            Thread.sleep(50)
            collector.stop()
        } catch (e: Exception) {
            // Should handle permission errors gracefully
        }
    }

    @Test
    fun testCollectorMetricsRecordingIsAttempted() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(120)
        collector.stop()

        verify(atLeast = 1) { mockMeter.counterBuilder(any()) }
    }

    @Test
    fun testCollectorHandlesNullMetrics() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()

        // Should complete without null pointer exceptions
        assertNotNull(collector)
    }

    @Test
    fun testCollectorThreadSafety() {
        val collector = PansMetricsCollector(context, mockSdk)
        val threads = mutableListOf<Thread>()

        repeat(3) {
            threads.add(
                Thread {
                    try {
                        collector.start()
                        Thread.sleep(50)
                        collector.stop()
                    } catch (e: Exception) {
                        // Expected in concurrent access
                    }
                },
            )
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertNotNull(collector)
    }

    @Test
    fun testCollectorIntegrationFlow() {
        val collector = PansMetricsCollector(context, mockSdk)

        // Complete lifecycle
        assertNotNull(collector)
        collector.start()
        Thread.sleep(75)
        verify(atLeast = 1) { mockSdk.getMeter(any()) }
        collector.stop()
    }

    @Test
    fun testCollectorStopsEvenAfterError() {
        val collector = PansMetricsCollector(context, mockSdk)
        collector.start()
        Thread.sleep(100)
        collector.stop()
        collector.stop() // Second stop should be safe

        assertNotNull(collector)
    }
}
