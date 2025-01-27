/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.common.internal.tools.time.SystemTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PeriodicRunnableTest {
    private lateinit var periodicWork: PeriodicWork
    private lateinit var testSystemTime: TestSystemTime

    @Before
    fun setUp() {
        periodicWork = createPeriodicWorkServiceMock()
        testSystemTime = TestSystemTime()
        SystemTime.setForTest(testSystemTime)
    }

    @Test
    fun `Run the first time right away`() {
        val minimumDelayInMillis = 1000L
        val runnable = createRunnable(minimumDelayInMillis)

        runnable.run()

        assertThat(runnable.timesRun).isEqualTo(1)
    }

    @Test
    fun `Wait minimum delay time before running again`() {
        val minimumDelayInMillis = 10_000L
        val runnable = createRunnable(minimumDelayInMillis)

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
        val runnable = createRunnable(1000)

        runnable.run()

        assertThat(runnable.timesRun).isEqualTo(1)
        verify {
            periodicWork.enqueue(runnable)
        }
    }

    @Test
    fun `When no need to run again, do not enqueue for next loop`() {
        val runnable = createRunnable(1000)
        runnable.stopAfterRun = true

        runnable.run()

        assertThat(runnable.timesRun).isEqualTo(1)
        verify(exactly = 0) {
            periodicWork.enqueue(runnable)
        }
    }

    private fun createRunnable(minimumDelayInMillis: Long) = TestRunnable(minimumDelayInMillis, periodicWork)

    private fun createPeriodicWorkServiceMock(): PeriodicWork {
        val periodicWork = mockk<PeriodicWork>()
        every { periodicWork.enqueue(any()) } just Runs

        return periodicWork
    }

    private class TestRunnable(
        val minimumDelayInMillis: Long,
        periodicWork: PeriodicWork,
    ) : PeriodicRunnable({ periodicWork }) {
        var timesRun = 0
        var stopAfterRun = false
        private var stopRunning = false

        override fun onRun() {
            timesRun++
            if (stopAfterRun) {
                stopRunning = true
            }
        }

        override fun shouldStopRunning(): Boolean = stopRunning

        override fun minimumDelayUntilNextRunInMillis(): Long = minimumDelayInMillis
    }

    private class TestSystemTime : SystemTime {
        var currentTime = 1000L

        override fun getCurrentTimeMillis(): Long = currentTime

        fun advanceTimeByMillis(millis: Long) {
            currentTime += millis
        }
    }
}
