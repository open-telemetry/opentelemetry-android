/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@ExtendWith(MockKExtension::class)
internal class AnrDetectorTogglerTest {
    @MockK
    private lateinit var anrWatcher: Runnable

    @MockK
    private lateinit var scheduler: ScheduledExecutorService

    @RelaxedMockK
    private lateinit var future: ScheduledFuture<*>

    private lateinit var underTest: AnrDetectorToggler

    @BeforeEach
    fun init() {
        underTest = AnrDetectorToggler(anrWatcher, scheduler)
    }

    @Test
    fun testOnApplicationForegrounded() {
        every {
            scheduler.scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS)
        } returns future

        underTest.onApplicationForegrounded()
        underTest.onApplicationForegrounded()
        underTest.onApplicationForegrounded()

        verify(exactly = 1) { scheduler.scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS) }
    }

    @Test
    fun testOnApplicationBackgrounded() {
        every {
            scheduler.scheduleWithFixedDelay(
                anrWatcher,
                1,
                1,
                TimeUnit.SECONDS,
            )
        } returns future

        underTest.onApplicationForegrounded()

        underTest.onApplicationBackgrounded()
        underTest.onApplicationBackgrounded()
        underTest.onApplicationBackgrounded()

        verify(exactly = 1) { future.cancel(true) }
    }
}
