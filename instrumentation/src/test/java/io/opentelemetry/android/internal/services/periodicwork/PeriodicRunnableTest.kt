/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.android.internal.tools.time.SystemTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PeriodicRunnableTest {
    private lateinit var periodicWorkService: PeriodicWorkService
    private lateinit var testSystemTime: TestSystemTime

    @Before
    fun setUp() {
        periodicWorkService = mock()
        val manager = mock<ServiceManager>()
        doReturn(periodicWorkService).whenever(manager).getService(PeriodicWorkService::class.java)
        ServiceManager.setForTest(manager)
        testSystemTime = TestSystemTime()
        SystemTime.setForTest(testSystemTime)
    }

    @Test
    fun `Run the first time right away`() {
        val minimumDelayInMillis = 1000L
        val runnable = TestRunnable(minimumDelayInMillis)

        runnable.run()

        assertThat(runnable.timesRun).isEqualTo(1)
    }

    @Test
    fun `Wait minimum delay time before running again`() {
        val minimumDelayInMillis = 10_000L
        val runnable = TestRunnable(minimumDelayInMillis)

        runnable.run()
        assertThat(runnable.timesRun).isEqualTo(1)

        // Try again right away (should not work)
        runnable.run()
        assertThat(runnable.timesRun).isEqualTo(1)

        // Wait for minimum delay
        testSystemTime.advanceTimeByMillis(testSystemTime.getCurrentTimeMillis() + minimumDelayInMillis)

        // Try again after the delay (should work)
        runnable.run()
        assertThat(runnable.timesRun).isEqualTo(2)
    }

    @Test
    fun `When needed to run again, enqueue for next loop`() {
        val runnable = TestRunnable(1000)

        runnable.run()

        assertThat(runnable.timesRun).isEqualTo(1)
        verify(periodicWorkService).enqueue(runnable)
    }

    @Test
    fun `When no need to run again, do not enqueue for next loop`() {
        val runnable = TestRunnable(1000)
        runnable.stopAfterRun = true

        runnable.run()

        assertThat(runnable.timesRun).isEqualTo(1)
        verify(periodicWorkService, never()).enqueue(runnable)
    }

    private class TestRunnable(val minimumDelayInMillis: Long) : PeriodicRunnable() {
        var timesRun = 0
        var stopAfterRun = false
        private var stopRunning = false

        override fun onRun() {
            timesRun++
            if (stopAfterRun) {
                stopRunning = true
            }
        }

        override fun shouldStopRunning(): Boolean {
            return stopRunning
        }

        override fun minimumDelayUntilNextRunInMillis(): Long {
            return minimumDelayInMillis
        }
    }

    private class TestSystemTime : SystemTime {
        var currentTime = 1000L

        override fun getCurrentTimeMillis(): Long {
            return currentTime
        }

        fun advanceTimeByMillis(millis: Long) {
            currentTime += millis
        }
    }
}
