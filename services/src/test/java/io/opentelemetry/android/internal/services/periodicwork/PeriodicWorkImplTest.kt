/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.periodicwork

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.common.internal.utils.threadIdCompat
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class PeriodicWorkImplTest {
    private lateinit var periodicWork: PeriodicWorkImpl

    @Before
    fun setUp() {
        periodicWork = PeriodicWorkImpl()
    }

    @After
    fun tearDown() {
        periodicWork.close()
    }

    @Test
    fun `enqueue and execute single task`() {
        val latch = CountDownLatch(1)
        var executed = false

        periodicWork.enqueue {
            executed = true
            latch.countDown()
        }

        fastForwardBySeconds(60)
        latch.await(5, TimeUnit.SECONDS)

        assertThat(executed).isTrue()
    }

    @Test
    fun `enqueue multiple tasks and execute all`() {
        val taskCount = 10
        val latch = CountDownLatch(taskCount)
        val executedCount = AtomicInteger(0)

        repeat(taskCount) {
            periodicWork.enqueue {
                executedCount.incrementAndGet()
                latch.countDown()
            }
        }

        fastForwardBySeconds(60)
        latch.await(5, TimeUnit.SECONDS)

        assertThat(executedCount.get()).isEqualTo(taskCount)
    }

    @Test
    fun `tasks execute in worker thread not main thread`() {
        val latch = CountDownLatch(1)
        var taskThreadId: Long = -1L
        val mainThreadId = findThreadId()

        periodicWork.enqueue {
            taskThreadId = findThreadId()
            latch.countDown()
        }

        fastForwardBySeconds(60)
        latch.await(5, TimeUnit.SECONDS)

        assertThat(taskThreadId).isNotEqualTo(-1L)
        assertThat(taskThreadId).isNotEqualTo(mainThreadId)
    }

    @Test
    fun `custom loop interval is used`() {
        val customIntervalMs = 5000L
        val customPeriodicWork = PeriodicWorkImpl(customIntervalMs)

        try {
            val latch = CountDownLatch(1)
            var executed = false

            customPeriodicWork.enqueue {
                executed = true
                latch.countDown()
            }

            // Fast forward by custom interval
            fastForwardByMillis(customIntervalMs)
            latch.await(5, TimeUnit.SECONDS)

            assertThat(executed).isTrue()
        } finally {
            customPeriodicWork.close()
        }
    }

    @Test
    fun `default loop interval constant is correct`() {
        assertThat(PeriodicWork.DEFAULT_LOOP_INTERVAL_MS).isEqualTo(10000L)
    }

    @Test
    fun `minimum loop interval constant is correct`() {
        assertThat(PeriodicWorkImpl.MINIMUM_LOOP_INTERVAL_MILLIS).isEqualTo(1000L)
    }

    @Test
    fun `enqueue does not execute task when closed`() {
        val latch = CountDownLatch(1)
        var executed = false

        periodicWork.close()

        periodicWork.enqueue {
            executed = true
            latch.countDown()
        }

        fastForwardBySeconds(60)
        val completed = latch.await(1, TimeUnit.SECONDS)

        assertThat(completed).isFalse()
        assertThat(executed).isFalse()
    }

    @Test
    fun `close is idempotent - can be called multiple times`() {
        // Enqueue some tasks first
        val executedCount = AtomicInteger(0)
        repeat(3) {
            periodicWork.enqueue {
                executedCount.incrementAndGet()
            }
        }

        // First close should succeed
        periodicWork.close()

        // Second close should also succeed (idempotent)
        periodicWork.close()

        // Third close should also succeed
        periodicWork.close()

        // Ensure no exception is thrown and tasks are not executed after close
        fastForwardBySeconds(60)

        // Tasks should not execute after close
        assertThat(executedCount.get()).isEqualTo(0)
    }

    @Test
    fun `close prevents new enqueues`() {
        val latch = CountDownLatch(1)
        var taskExecuted = false

        periodicWork.close()

        periodicWork.enqueue {
            taskExecuted = true
            latch.countDown()
        }

        fastForwardBySeconds(60)
        val completed = latch.await(500, TimeUnit.MILLISECONDS)

        assertThat(completed).isFalse()
        assertThat(taskExecuted).isFalse()
    }

    @Test
    fun `tasks enqueued after close are not executed on subsequent loops`() {
        periodicWork.close()

        val executedTasks = mutableListOf<Int>()
        repeat(5) { i ->
            periodicWork.enqueue {
                executedTasks.add(i)
            }
        }

        // Fast forward multiple loop intervals
        fastForwardBySeconds(120)

        assertThat(executedTasks).isEmpty()
    }

    @Test
    fun `periodic check for work continues across multiple intervals`() {
        val firstRunLatch = CountDownLatch(1)
        val secondRunLatch = CountDownLatch(1)
        var firstExecuted = false
        var secondExecuted = false

        // First task
        periodicWork.enqueue {
            firstExecuted = true
            firstRunLatch.countDown()
        }

        fastForwardBySeconds(60)
        firstRunLatch.await(5, TimeUnit.SECONDS)
        assertThat(firstExecuted).isTrue()

        // Add second task after first completes
        periodicWork.enqueue {
            secondExecuted = true
            secondRunLatch.countDown()
        }

        fastForwardBySeconds(60)
        secondRunLatch.await(5, TimeUnit.SECONDS)
        assertThat(secondExecuted).isTrue()
    }

    @Test
    fun `tasks are delegated to single worker thread`() {
        val taskCount = 5
        val latch = CountDownLatch(taskCount)
        val threadIds = mutableSetOf<Long>()
        val lock = Any()

        repeat(taskCount) {
            periodicWork.enqueue {
                synchronized(lock) {
                    threadIds.add(findThreadId())
                }
                latch.countDown()
            }
        }

        fastForwardBySeconds(60)
        latch.await(5, TimeUnit.SECONDS)

        // All tasks should have run in the same worker thread
        assertThat(threadIds.size).isEqualTo(1)
    }

    @Test
    fun `work is removed from queue after execution`() {
        val executionCounts = mutableMapOf<Int, Int>()
        val lock = Any()
        val taskCount = 3
        val latch = CountDownLatch(taskCount)

        repeat(taskCount) { i ->
            periodicWork.enqueue {
                synchronized(lock) {
                    executionCounts[i] = (executionCounts[i] ?: 0) + 1
                }
                latch.countDown()
            }
        }

        fastForwardBySeconds(60)
        latch.await(5, TimeUnit.SECONDS)

        // Fast forward again to ensure tasks don't re-execute
        fastForwardBySeconds(60)

        // Each task should have executed exactly once
        executionCounts.values.forEach { count ->
            assertThat(count).isEqualTo(1)
        }
    }

    @Test
    fun `tasks execute in order they were enqueued`() {
        val executionOrder = mutableListOf<Int>()
        val lock = Any()
        val taskCount = 5
        val latch = CountDownLatch(taskCount)

        repeat(taskCount) { i ->
            periodicWork.enqueue {
                synchronized(lock) {
                    executionOrder.add(i)
                }
                latch.countDown()
            }
        }

        fastForwardBySeconds(60)
        latch.await(5, TimeUnit.SECONDS)

        assertThat(executionOrder).isEqualTo(listOf(0, 1, 2, 3, 4))
    }

    @Test
    fun `enqueue task after previous batch completed`() {
        val firstLatch = CountDownLatch(1)
        val secondLatch = CountDownLatch(1)
        var firstCompleted = false
        var secondCompleted = false

        periodicWork.enqueue {
            firstCompleted = true
            firstLatch.countDown()
        }

        fastForwardBySeconds(60)
        firstLatch.await(5, TimeUnit.SECONDS)
        assertThat(firstCompleted).isTrue()

        // Enqueue after first completed
        periodicWork.enqueue {
            secondCompleted = true
            secondLatch.countDown()
        }

        fastForwardBySeconds(60)
        secondLatch.await(5, TimeUnit.SECONDS)
        assertThat(secondCompleted).isTrue()
    }

    @Test
    fun `verify close clears remaining tasks`() {
        val executedTasks = AtomicInteger(0)

        // Enqueue tasks
        repeat(10) {
            periodicWork.enqueue {
                executedTasks.incrementAndGet()
            }
        }

        // Close immediately before tasks can execute
        periodicWork.close()

        // Fast forward to trigger any remaining work
        fastForwardBySeconds(60)

        // Tasks should not execute after close
        assertThat(executedTasks.get()).isEqualTo(0)
    }

    @Test
    fun `close called twice clears queue on second call`() {
        val executedTasks = AtomicInteger(0)

        // First close
        periodicWork.close()

        // Try to enqueue after first close (should be rejected)
        repeat(5) {
            periodicWork.enqueue {
                executedTasks.incrementAndGet()
            }
        }

        // Second close - should hit the queue.clear() path since compareAndSet returns false
        periodicWork.close()

        fastForwardBySeconds(60)

        assertThat(executedTasks.get()).isEqualTo(0)
    }

    private fun findThreadId(): Long {
        val thread = Thread.currentThread()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            thread.threadId()
        } else {
            thread.threadIdCompat
        }
    }

    private fun fastForwardBySeconds(seconds: Long) {
        ShadowLooper.idleMainLooper(seconds, TimeUnit.SECONDS)
    }

    private fun fastForwardByMillis(millis: Long) {
        ShadowLooper.idleMainLooper(millis, TimeUnit.MILLISECONDS)
    }
}
