/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class PeriodicWorkTest {
    companion object {
        private const val DELAY_BETWEEN_EXECUTIONS_IN_SECONDS = 10L
    }

    private lateinit var service: PeriodicWork

    @Before
    fun setUp() {
        service = PeriodicWork()
    }

    @Test
    fun `Execute enqueued work on start`() {
        val numberOfTasks = 5
        val latch = CountDownLatch(numberOfTasks)
        val threadIds = mutableSetOf<Long>()
        repeat(numberOfTasks) {
            service.enqueue {
                threadIds.add(Thread.currentThread().id)
                latch.countDown()
            }
        }

        fastForwardBySeconds(DELAY_BETWEEN_EXECUTIONS_IN_SECONDS)
        latch.await()

        // All ran in a single worker thread
        assertThat(threadIds.size).isEqualTo(1)

        // The worker thread is not the same as the main thread
        assertThat(threadIds.first()).isNotEqualTo(Thread.currentThread().id)
    }

    @Test
    fun `Check for pending work after a delay`() {
        val firstRunLatch = CountDownLatch(1)
        val secondRunLatch = CountDownLatch(1)
        var secondRunExecuted = false

        // First run right away
        service.enqueue {
            firstRunLatch.countDown()
        }
        fastForwardBySeconds(DELAY_BETWEEN_EXECUTIONS_IN_SECONDS)
        service.enqueue {
            secondRunExecuted = true
            secondRunLatch.countDown()
        }
        firstRunLatch.await()
        assertThat(secondRunExecuted).isFalse()

        // Second run after delay
        fastForwardBySeconds(DELAY_BETWEEN_EXECUTIONS_IN_SECONDS)
        secondRunLatch.await(1, TimeUnit.SECONDS)
        assertThat(secondRunExecuted).isTrue()
    }

    @Test
    fun `Remove delegated work from further executions`() {
        val firstRunLatch = CountDownLatch(1)
        val secondRunLatch = CountDownLatch(1)
        var timesExecutedFirstWork = 0
        var timesExecutedSecondWork = 0

        // First run right away
        service.enqueue {
            timesExecutedFirstWork++
            firstRunLatch.countDown()
        }
        fastForwardBySeconds(DELAY_BETWEEN_EXECUTIONS_IN_SECONDS)
        service.enqueue {
            timesExecutedSecondWork++
            secondRunLatch.countDown()
        }
        firstRunLatch.await()
        assertThat(timesExecutedFirstWork).isEqualTo(1)
        assertThat(timesExecutedSecondWork).isEqualTo(0)

        // Second run after delay
        fastForwardBySeconds(DELAY_BETWEEN_EXECUTIONS_IN_SECONDS)
        secondRunLatch.await(1, TimeUnit.SECONDS)
        assertThat(timesExecutedFirstWork).isEqualTo(1)
        assertThat(timesExecutedSecondWork).isEqualTo(1)
    }

    private fun fastForwardBySeconds(seconds: Long) {
        ShadowLooper.idleMainLooper(seconds, TimeUnit.SECONDS)
    }
}
