/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodic

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PeriodicTaskSchedulerImplTest {
    @Test
    fun `Run periodic work until stopped`() = runTest {
        val runCount = AtomicInteger()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scheduler = PeriodicTaskSchedulerImpl(dispatcher)

        scheduler.start(
            object : PeriodicRunnable {
                override fun period() = 10.milliseconds

                override fun run() {
                    runCount.incrementAndGet()
                }

                override fun stop() {}

                override fun shouldStop(): Boolean = runCount.get() == 3
            },
        )

        advanceUntilIdle()

        assertThat(runCount.get()).isEqualTo(3)
        scheduler.close()
    }

    @Test
    fun `Cancel scheduled work when closed`() = runTest {
        val runCount = AtomicInteger()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scheduler = PeriodicTaskSchedulerImpl(dispatcher)

        scheduler.start(
            object : PeriodicRunnable {
                override fun period() = 10.milliseconds

                override fun run() {
                    runCount.incrementAndGet()
                }

                override fun stop() {}

                override fun shouldStop(): Boolean = false
            },
        )

        runCurrent()
        assertThat(runCount.get()).isEqualTo(1)

        scheduler.close()
        advanceTimeBy(1_000)
        advanceUntilIdle()
        assertThat(runCount.get()).isEqualTo(1)
    }

    @Test
    fun `Continue periodic work after exception`() = runTest {
        val runCount = AtomicInteger()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scheduler = PeriodicTaskSchedulerImpl(dispatcher)

        scheduler.start(
            object : PeriodicRunnable {
                override fun period() = 10.milliseconds

                override fun run() {
                    if (runCount.incrementAndGet() == 2) {
                        throw IOException("kaboom")
                    }
                }

                override fun stop() {}

                override fun shouldStop(): Boolean = runCount.get() == 3
            },
        )

        advanceUntilIdle()

        assertThat(runCount.get()).isEqualTo(3)
        scheduler.close()
    }
}
