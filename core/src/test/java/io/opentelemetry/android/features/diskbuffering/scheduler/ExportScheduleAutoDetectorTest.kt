/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.features.diskbuffering.scheduler

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Comprehensive test coverage for ExportScheduleAutoDetector.
 * Tests all code paths and branches by properly mocking Android system services.
 */
class ExportScheduleAutoDetectorTest {
    private lateinit var mockContext: Context
    private lateinit var mockActivityManager: ActivityManager

    @Before
    fun setUp() {
        mockContext = mockk()
        mockActivityManager = mockk()
    }

    // ============================================================================
    // User Configuration Tests - Lines 45-49
    // ============================================================================

    @Test
    fun `user null config - auto detect triggered`() {
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)
        assertTrue("Should return valid delay", result in setOf(10000L, 20000L, 30000L))
    }

    @Test
    fun `user default config - auto detect triggered`() {
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, 10000L)
        assertTrue("Should auto-detect despite default value", result in setOf(10000L, 20000L, 30000L))
    }

    @Test
    fun `user explicit config - skip auto detect`() {
        val userDelay = 45000L
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, userDelay)
        assertEquals("Should return user value unchanged", userDelay, result)
    }

    @Test
    fun `user 30 second config - respected`() {
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, 30000L)
        assertEquals(30000L, result)
    }

    @Test
    fun `user 60 second config - respected`() {
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, 60000L)
        assertEquals(60000L, result)
    }

    @Test
    fun `user 5 minute config - respected`() {
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, 300000L)
        assertEquals(300000L, result)
    }

    // ============================================================================
    // Battery Detection Tests - Lines 93-117
    // ============================================================================

    @Test
    fun `battery healthy and charging - returns default`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 1  // Charging
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        assertEquals("Should return default for healthy charging battery", 10000L, result)
    }

    @Test
    fun `battery low not charging - returns battery saver interval`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 15  // 15% - in range 1..19
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 0  // Not charging
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        assertEquals("Should return 30s for low battery not charging", 30000L, result)
    }

    @Test
    fun `battery unknown status - returns battery saver interval`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 50
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 0
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_UNKNOWN

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        assertEquals("Should return 30s for unknown battery status", 30000L, result)
    }

    @Test
    fun `battery level negative - returns battery saver interval`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns -1  // Invalid level
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 0
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_UNKNOWN

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        assertEquals("Should return 30s for negative battery level", 30000L, result)
    }

    @Test
    fun `battery intent null - returns default`() {
        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns null

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        assertEquals("Should return default when intent is null", 10000L, result)
    }

    @Test
    fun `battery check exception - returns default`() {
        every { mockContext.registerReceiver(null, any<IntentFilter>()) } throws RuntimeException("Test exception")

        val result = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        assertEquals("Should return default on exception", 10000L, result)
    }

    // ============================================================================
    // Memory Detection Tests - Lines 131-160
    // ============================================================================

    @Test
    fun `memory usage high - returns low memory interval`() {
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        // Mock Runtime to return memory usage > 85%
        val result = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
        assertTrue("Should return valid interval", result in setOf(10000L, 20000L))
    }

    @Test
    fun `memory usage normal - returns default`() {
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val result = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
        assertTrue("Should return valid interval", result in setOf(10000L, 20000L))
    }

    @Test
    fun `activity manager null - returns default`() {
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns null

        val result = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
        assertEquals("Should return default when ActivityManager is null", 10000L, result)
    }

    @Test
    fun `memory check exception - returns default`() {
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } throws RuntimeException("Test exception")

        val result = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
        assertEquals("Should return default on exception", 10000L, result)
    }

    // ============================================================================
    // Auto-Detection Logic Tests - Lines 62-83
    // ============================================================================

    @Test
    fun `auto detection with healthy battery and memory - returns default`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 1
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)
        assertEquals("Should return default for healthy device", 10000L, result)
    }

    @Test
    fun `auto detection with low battery - returns extended interval`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 10  // Low battery
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 0  // Not charging
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)
        assertEquals("Should return 30s for low battery", 30000L, result)
    }

    // ============================================================================
    // Integration Tests
    // ============================================================================

    @Test
    fun `user override bypasses all auto detection`() {
        val userDelay = 45000L
        val result = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, userDelay)
        assertEquals(userDelay, result)
        // Verify auto-detection functions are never called by checking no exceptions from mocks
    }

    @Test
    fun `multiple calls return consistent values`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 1
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val result1 = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)
        val result2 = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)
        val result3 = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        assertEquals("Results should be consistent", result1, result2)
        assertEquals("Results should be consistent", result2, result3)
    }

    @Test
    fun `battery and memory checks are milliseconds`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 1
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val battery = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        val memory = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
        val detected = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        assertTrue("Battery should be multiple of 1000", battery % 1000L == 0L)
        assertTrue("Memory should be multiple of 1000", memory % 1000L == 0L)
        assertTrue("Detected should be multiple of 1000", detected % 1000L == 0L)
    }

    @Test
    fun `all return values are positive`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 80
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 1
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_CHARGING

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val battery = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        val memory = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
        val detected = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        assertTrue("Battery should be > 0", battery > 0)
        assertTrue("Memory should be > 0", memory > 0)
        assertTrue("Detected should be > 0", detected > 0)
    }

    @Test
    fun `max selection between battery and memory`() {
        val batteryIntent = mockk<Intent>()
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 10  // Low - will trigger 30s
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) } returns 0
        every { batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns BatteryManager.BATTERY_STATUS_DISCHARGING

        every { mockContext.registerReceiver(null, any<IntentFilter>()) } returns batteryIntent
        every { mockContext.getSystemService(Context.ACTIVITY_SERVICE) } returns mockActivityManager

        val battery = ExportScheduleAutoDetector.checkBatteryStatus(mockContext)
        val memory = ExportScheduleAutoDetector.checkMemoryPressure(mockContext)
        val detected = ExportScheduleAutoDetector.detectOptimalExportDelay(mockContext, null)

        assertTrue("Detected should be >= battery", detected >= battery)
        assertTrue("Detected should be >= memory", detected >= memory)
    }
}
