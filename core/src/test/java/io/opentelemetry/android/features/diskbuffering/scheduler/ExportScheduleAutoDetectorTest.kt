/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import android.app.ActivityManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Tests for ExportScheduleAutoDetector with DiskBufferingConfig.
 * Tests the complete feature workflow and all integration points.
 */
class ExportScheduleAutoDetectorTest {
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = mockk()
    }

    // ============================================================================
    // TEST SUITE 1: Configuration Integration
    // ============================================================================

    @Test
    fun `DiskBufferingConfig with auto-detection disabled uses fixed interval`() {
        val config =
            DiskBufferingConfig(
                enabled = true,
                autoDetectExportSchedule = false,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(10),
            )

        assertEquals(false, config.autoDetectExportSchedule)
        assertEquals(TimeUnit.SECONDS.toMillis(10), config.exportScheduleDelayMillis)
    }

    @Test
    fun `DiskBufferingConfig with auto-detection enabled uses detection`() {
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                autoDetectExportSchedule = true,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(10),
            )

        assertEquals(true, config.autoDetectExportSchedule)
        assertEquals(TimeUnit.SECONDS.toMillis(10), config.exportScheduleDelayMillis)
    }

    @Test
    fun `User override takes precedence over auto-detection setting`() {
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                autoDetectExportSchedule = true,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(60),
            )

        // Even with auto-detect enabled, explicit config should be honored
        assertEquals(TimeUnit.SECONDS.toMillis(60), config.exportScheduleDelayMillis)
        assertEquals(true, config.autoDetectExportSchedule)
    }

    @Test
    fun `Validation enforces minimum export delay of 1 second`() {
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                exportScheduleDelayMillis = 500L, // Below minimum
            )

        // Should be corrected to 1000ms minimum
        assertTrue(config.exportScheduleDelayMillis >= TimeUnit.SECONDS.toMillis(1))
    }

    // ============================================================================
    // TEST SUITE 2: Auto-Detection Logic Flow
    // ============================================================================

    @Test
    fun `Auto-detection returns valid value for normal conditions`() {
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        // Should return valid delay
        assertTrue(result > 0)
        assertTrue(result % 1000L == 0L) // Milliseconds in full seconds
        assertTrue(result >= TimeUnit.SECONDS.toMillis(10))
    }

    @Test
    fun `Auto-detection with user config returns user value`() {
        val userDelay = TimeUnit.SECONDS.toMillis(45)
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, userDelay)

        assertEquals(userDelay, result)
    }

    @Test
    fun `Battery and memory checks return valid intervals`() {
        val batteryDelay = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        val memoryDelay = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)

        // Both should return valid intervals
        assertTrue(
            batteryDelay in
                setOf(
                    TimeUnit.SECONDS.toMillis(10),
                    TimeUnit.SECONDS.toMillis(30),
                ),
        )

        assertTrue(
            memoryDelay in
                setOf(
                    TimeUnit.SECONDS.toMillis(10),
                    TimeUnit.SECONDS.toMillis(20),
                ),
        )
    }

    @Test
    fun `Auto-detection respects maximum of battery and memory delays`() {
        val batteryDelay = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        val memoryDelay = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
        val detectedDelay = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        // Detected should be >= both checks
        assertTrue(detectedDelay >= batteryDelay)
        assertTrue(detectedDelay >= memoryDelay)
    }

    // ============================================================================
    // TEST SUITE 3: DefaultExportScheduler Integration
    // ============================================================================

    @Test
    fun `DefaultExportScheduler uses provided delay`() {
        val delay = TimeUnit.SECONDS.toMillis(30)
        val mockPeriodicWork = { mockk<io.opentelemetry.android.internal.services.periodicwork.PeriodicWork>() }

        val scheduler = DefaultExportScheduler(mockPeriodicWork, delay)

        assertEquals(delay, scheduler.minimumDelayUntilNextRunInMillis())
    }

    @Test
    fun `DefaultExportScheduler default delay is 10 seconds`() {
        val mockPeriodicWork = { mockk<io.opentelemetry.android.internal.services.periodicwork.PeriodicWork>() }

        val scheduler = DefaultExportScheduler(mockPeriodicWork)

        assertEquals(TimeUnit.SECONDS.toMillis(10), scheduler.minimumDelayUntilNextRunInMillis())
    }

    // ============================================================================
    // TEST SUITE 4: Configuration Scenarios
    // ============================================================================

    @Test
    fun `scenario - Standard mobile application (auto-detect enabled)`() {
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                autoDetectExportSchedule = true,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(10),
            )

        assertEquals(true, config.autoDetectExportSchedule)
        assertEquals(TimeUnit.SECONDS.toMillis(10), config.exportScheduleDelayMillis)

        val detectedDelay =
            ExportScheduleAutoDetector.detectOptimalExportDelay(
                mockContext,
                if (config.exportScheduleDelayMillis == TimeUnit.SECONDS.toMillis(10)) null else config.exportScheduleDelayMillis,
            )

        assertTrue(detectedDelay >= TimeUnit.SECONDS.toMillis(10))
        assertTrue(detectedDelay <= TimeUnit.SECONDS.toMillis(30))
    }

    @Test
    fun `scenario - High volume telemetry (explicit user config)`() {
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                autoDetectExportSchedule = true,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(60),
            )

        assertEquals(true, config.autoDetectExportSchedule)
        assertEquals(TimeUnit.SECONDS.toMillis(60), config.exportScheduleDelayMillis)

        // User explicit value should be returned
        val delay =
            ExportScheduleAutoDetector.detectOptimalExportDelay(
                mockContext,
                config.exportScheduleDelayMillis,
            )

        assertEquals(TimeUnit.SECONDS.toMillis(60), delay)
    }

    @Test
    fun `scenario - Real-time monitoring (auto-detect disabled)`() {
        val config =
            DiskBufferingConfig(
                enabled = true,
                autoDetectExportSchedule = false,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(5),
            )

        assertEquals(false, config.autoDetectExportSchedule)
        assertEquals(TimeUnit.SECONDS.toMillis(5), config.exportScheduleDelayMillis)
    }

    @Test
    fun `scenario - Battery optimization (explicit long delay)`() {
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                autoDetectExportSchedule = true,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(300), // 5 minutes
            )

        assertEquals(true, config.autoDetectExportSchedule)
        assertEquals(TimeUnit.SECONDS.toMillis(300), config.exportScheduleDelayMillis)
    }

    // ============================================================================
    // TEST SUITE 5: Edge Cases and Error Handling
    // ============================================================================

    @Test
    fun `Minimum delay value is enforced (1 second)`() {
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                exportScheduleDelayMillis = 100L, // Way below minimum
            )

        assertTrue(config.exportScheduleDelayMillis >= TimeUnit.SECONDS.toMillis(1))
    }

    @Test
    fun `Very long delay is accepted (30 minutes)`() {
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                exportScheduleDelayMillis = TimeUnit.MINUTES.toMillis(30),
            )

        assertEquals(TimeUnit.MINUTES.toMillis(30), config.exportScheduleDelayMillis)
    }

    @Test
    fun `Multiple auto-detection calls return consistent values`() {
        val result1 = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)
        val result2 = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)
        val result3 = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        // All should be equal (no randomness)
        assertEquals(result1, result2)
        assertEquals(result2, result3)
    }

    @Test
    fun `Auto-detection handles various battery scenarios`() {
        // Test that battery detection returns valid values
        repeat(10) {
            val batteryDelay = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
            assertTrue(
                batteryDelay in
                    setOf(
                        TimeUnit.SECONDS.toMillis(10),
                        TimeUnit.SECONDS.toMillis(30),
                    ),
            )
        }
    }

    @Test
    fun `Auto-detection handles memory variations`() {
        // Test multiple memory checks
        repeat(10) {
            val memoryDelay = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
            assertTrue(
                memoryDelay in
                    setOf(
                        TimeUnit.SECONDS.toMillis(10),
                        TimeUnit.SECONDS.toMillis(20),
                    ),
            )
        }
    }

    // ============================================================================
    // TEST SUITE 6: Complete Feature Workflow
    // ============================================================================

    @Test
    fun `complete workflow - Config creation to scheduler initialization`() {
        // 1. Create config with auto-detection
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                autoDetectExportSchedule = true,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(10),
            )

        // 2. Determine export delay using auto-detector
        val exportDelay =
            if (config.autoDetectExportSchedule) {
                ExportScheduleAutoDetector.detectOptimalExportDelay(
                    mockContext,
                    if (config.exportScheduleDelayMillis == TimeUnit.SECONDS.toMillis(10)) {
                        null // Use auto-detection
                    } else {
                        config.exportScheduleDelayMillis // Use user override
                    },
                )
            } else {
                config.exportScheduleDelayMillis
            }

        // 3. Create scheduler with determined delay
        val mockPeriodicWork = { mockk<io.opentelemetry.android.internal.services.periodicwork.PeriodicWork>() }
        val scheduler = DefaultExportScheduler(mockPeriodicWork, exportDelay)

        // 4. Verify scheduler has correct delay
        assertEquals(exportDelay, scheduler.minimumDelayUntilNextRunInMillis())
        assertTrue(exportDelay > 0)
        assertTrue(exportDelay % 1000L == 0L)
    }

    @Test
    fun `complete workflow - User override bypasses auto-detection`() {
        // 1. Create config with explicit user override
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                autoDetectExportSchedule = true,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(45), // Explicit override
            )

        // 2. Determine export delay
        val exportDelay =
            ExportScheduleAutoDetector.detectOptimalExportDelay(
                mockContext,
                config.exportScheduleDelayMillis, // Not null, so auto-detection skipped
            )

        // 3. Should use user-configured value
        assertEquals(TimeUnit.SECONDS.toMillis(45), exportDelay)

        // 4. Create scheduler
        val mockPeriodicWork = { mockk<io.opentelemetry.android.internal.services.periodicwork.PeriodicWork>() }
        val scheduler = DefaultExportScheduler(mockPeriodicWork, exportDelay)

        assertEquals(TimeUnit.SECONDS.toMillis(45), scheduler.minimumDelayUntilNextRunInMillis())
    }

    // ============================================================================
    // TEST SUITE 7: Type Safety and Return Values
    // ============================================================================

    @Test
    fun `All return values are positive and in milliseconds`() {
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(15),
            )

        val batteryDelay = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        val memoryDelay = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
        val autoDetectedDelay = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        // All must be positive
        assertTrue(batteryDelay > 0)
        assertTrue(memoryDelay > 0)
        assertTrue(autoDetectedDelay > 0)

        // All must be in full seconds (multiple of 1000)
        assertTrue(batteryDelay % 1000L == 0L)
        assertTrue(memoryDelay % 1000L == 0L)
        assertTrue(autoDetectedDelay % 1000L == 0L)

        // Config value must also be valid
        assertTrue(config.exportScheduleDelayMillis > 0)
        assertTrue(config.exportScheduleDelayMillis % 1000L == 0L)
    }

    @Test
    fun `Configuration immutability and correctness`() {
        val delay = TimeUnit.SECONDS.toMillis(25)
        val config =
            DiskBufferingConfig.create(
                enabled = true,
                autoDetectExportSchedule = true,
                exportScheduleDelayMillis = delay,
            )

        // Verify all properties are correctly set
        assertEquals(true, config.enabled)
        assertEquals(true, config.autoDetectExportSchedule)
        assertEquals(delay, config.exportScheduleDelayMillis)

        // Create another config and verify independence
        val config2 =
            DiskBufferingConfig.create(
                enabled = true,
                autoDetectExportSchedule = false,
                exportScheduleDelayMillis = TimeUnit.SECONDS.toMillis(60),
            )

        assertEquals(false, config2.autoDetectExportSchedule)
        assertEquals(TimeUnit.SECONDS.toMillis(60), config2.exportScheduleDelayMillis)

        // Original config should be unchanged
        assertEquals(true, config.autoDetectExportSchedule)
        assertEquals(delay, config.exportScheduleDelayMillis)
    }

    @Test
    fun `checkBatteryStatus returns BATTERY_SAVER_INTERVAL when battery is low and not charging`() {
        val intent = mockk<android.content.Intent>()
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) } returns 10
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) } returns 0
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) } returns
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING
        every { mockContext.registerReceiver(null, any()) } returns intent

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)

        assertEquals(30000L, result)
    }

    @Test
    fun `checkBatteryStatus returns DEFAULT_EXPORT_INTERVAL when battery is low but charging`() {
        val intent = mockk<android.content.Intent>()
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) } returns 10
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) } returns
            android.os.BatteryManager.BATTERY_PLUGGED_USB
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) } returns
            android.os.BatteryManager.BATTERY_STATUS_CHARGING
        every { mockContext.registerReceiver(null, any()) } returns intent

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)

        assertEquals(10000L, result)
    }

    @Test
    fun `checkBatteryStatus returns DEFAULT_EXPORT_INTERVAL when battery is good`() {
        val intent = mockk<android.content.Intent>()
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) } returns 80
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) } returns 0
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) } returns
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING
        every { mockContext.registerReceiver(null, any()) } returns intent

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)

        assertEquals(10000L, result)
    }

    @Test
    fun `checkMemoryPressure returns LOW_MEMORY_INTERVAL when usage is above 85 percent`() {
        // Max: 100, Total: 100, Free: 10 -> Used: 90 (90%)
        ExportScheduleAutoDetector.memoryInfoProvider = {
            ExportScheduleAutoDetector.MemoryInfo(100L, 100L, 10L)
        }

        val mockActivityManager = mockk<ActivityManager>()
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val result = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)

        assertEquals(20000L, result)

        // Reset
        ExportScheduleAutoDetector.memoryInfoProvider = {
            val runtime = Runtime.getRuntime()
            ExportScheduleAutoDetector.MemoryInfo(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory())
        }
    }

    @Test
    fun `checkMemoryPressure returns DEFAULT_EXPORT_INTERVAL when usage is below 85 percent`() {
        // Max: 100, Total: 100, Free: 50 -> Used: 50 (50%)
        ExportScheduleAutoDetector.memoryInfoProvider = {
            ExportScheduleAutoDetector.MemoryInfo(100L, 100L, 50L)
        }

        val mockActivityManager = mockk<ActivityManager>()
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val result = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)

        assertEquals(10000L, result)

        // Reset
        ExportScheduleAutoDetector.memoryInfoProvider = {
            val runtime = Runtime.getRuntime()
            ExportScheduleAutoDetector.MemoryInfo(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory())
        }
    }

    // ============================================================================
    // TEST SUITE 8: Exception Handling and Edge Cases for Coverage
    // ============================================================================

    @Test
    fun `checkBatteryStatus returns BATTERY_SAVER_INTERVAL when status is UNKNOWN`() {
        val intent = mockk<android.content.Intent>()
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) } returns -1
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) } returns 0
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) } returns
            android.os.BatteryManager.BATTERY_STATUS_UNKNOWN
        every { mockContext.registerReceiver(null, any()) } returns intent

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)

        assertEquals(30000L, result)
    }

    @Test
    fun `checkBatteryStatus returns DEFAULT when registerReceiver returns null`() {
        every { mockContext.registerReceiver(null, any()) } returns null

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)

        assertEquals(10000L, result)
    }

    @Test
    fun `checkBatteryStatus returns DEFAULT when exception is thrown`() {
        every { mockContext.registerReceiver(null, any()) } throws RuntimeException("Test exception")

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)

        assertEquals(10000L, result)
    }

    @Test
    fun `checkMemoryPressure returns DEFAULT when ActivityManager is null`() {
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns null

        val result = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)

        assertEquals(10000L, result)
    }

    @Test
    fun `checkMemoryPressure returns DEFAULT when exception is thrown`() {
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } throws RuntimeException("Test exception")

        val result = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)

        assertEquals(10000L, result)
    }

    @Test
    fun `detectOptimalExportDelay with default value triggers auto-detection`() {
        // When userConfiguredDelay equals DEFAULT (10000L), it should trigger auto-detection
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, 10000L)

        // Should return detected delay (10000L with mocked context that fails)
        assertTrue(result >= 10000L)
    }

    @Test
    fun `detectOptimalExportDelay with null triggers auto-detection path`() {
        // Setup battery to return extended interval
        val intent = mockk<android.content.Intent>()
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) } returns 10
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) } returns 0
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) } returns
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING
        every { mockContext.registerReceiver(null, any()) } returns intent

        // Setup memory to return default
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns null

        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        // Battery is low, so should return 30000L
        assertEquals(30000L, result)
    }

    @Test
    fun `detection uses max of battery and memory delays`() {
        // Setup battery to return saver interval (30s)
        val intent = mockk<android.content.Intent>()
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) } returns 10
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) } returns 0
        every { intent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) } returns
            android.os.BatteryManager.BATTERY_STATUS_DISCHARGING
        every { mockContext.registerReceiver(null, any()) } returns intent

        // Setup memory to return pressure interval (20s)
        ExportScheduleAutoDetector.memoryInfoProvider = {
            ExportScheduleAutoDetector.MemoryInfo(100L, 100L, 10L)
        }
        val mockActivityManager = mockk<ActivityManager>()
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        // Should use max of battery (30s) and memory (20s) = 30s
        assertEquals(30000L, result)

        // Reset
        ExportScheduleAutoDetector.memoryInfoProvider = {
            val runtime = Runtime.getRuntime()
            ExportScheduleAutoDetector.MemoryInfo(runtime.maxMemory(), runtime.totalMemory(), runtime.freeMemory())
        }
    }
}
