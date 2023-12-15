/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class PeriodicWorkServiceTest {
    companion object {
        private const val DELAY_BETWEEN_EXECUTIONS_IN_SECONDS = 10L
    }

    private lateinit var service: PeriodicWorkService

    @Before
    fun setUp() {
        service = PeriodicWorkService()
    }

    @Test
    fun `Execute enqueued work on start`() {
        val latch = CountDownLatch(1)
        var workerThreadId: Long? = null
        service.enqueue {
            workerThreadId = Thread.currentThread().id
            latch.countDown()
        }

        service.start()
        latch.await()

        assertThat(workerThreadId).isNotNull()
        assertThat(workerThreadId).isNotEqualTo(Thread.currentThread().id)
    }

    @Test
    fun `Start only once`() {
        val latch = CountDownLatch(1)

        // First start (should work)
        service.enqueue {
            latch.countDown()
        }
        service.start()
        latch.await()

        // Trying to re-start (should not work)
        service.enqueue {
            fail("Must not execute this right away.")
        }
        service.start()

        Thread.sleep(200) // Giving some time for the test to fail.
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
        service.start()
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
        service.start()
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
