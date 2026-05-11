/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodic

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PeriodicTaskSchedulerImplTest {
    @Test
    fun `Run periodic work until stopped`() {
        val scheduler = PeriodicTaskSchedulerImpl()
        val runCount = AtomicInteger()
        val completion = CountDownLatch(3)

        scheduler.start(
            object : PeriodicRunnable {
                override fun period() = 10.milliseconds

                override fun run() {
                    if (runCount.incrementAndGet() <= 3) {
                        completion.countDown()
                    }
                }

                override fun stop() {}

                override fun shouldStop(): Boolean = runCount.get() >= 3
            },
        )

        assertThat(completion.await(1, TimeUnit.SECONDS)).isTrue()
        assertThat(runCount.get()).isGreaterThanOrEqualTo(3)
        scheduler.close()
    }

    @Test
    fun `Cancel scheduled work when closed`() {
        val scheduler = PeriodicTaskSchedulerImpl()
        val runCount = AtomicInteger()
        val firstRun = CountDownLatch(1)

        scheduler.start(
            object : PeriodicRunnable {
                override fun period() = 10.milliseconds

                override fun run() {
                    runCount.incrementAndGet()
                    scheduler.close()
                    firstRun.countDown()
                }

                override fun stop() {}

                override fun shouldStop(): Boolean = false
            },
        )

        assertThat(firstRun.await(1, TimeUnit.SECONDS)).isTrue()
        Thread.sleep(250) // if still running we might get more increments

        assertThat(runCount.get()).isEqualTo(1)
    }
}
