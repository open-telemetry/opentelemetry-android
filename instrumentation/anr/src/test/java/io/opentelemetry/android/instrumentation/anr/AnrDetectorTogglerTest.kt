/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@ExtendWith(MockitoExtension::class)
internal class AnrDetectorTogglerTest {
    @Mock
    private lateinit var anrWatcher: Runnable

    @Mock
    private lateinit var scheduler: ScheduledExecutorService

    @Mock
    private lateinit var future: ScheduledFuture<*>

    @InjectMocks
    private lateinit var underTest: AnrDetectorToggler

    @Test
    fun testOnApplicationForegrounded() {
        Mockito
            .doReturn(future)
            .`when`(scheduler)
            .scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS)

        underTest.onApplicationForegrounded()
        underTest.onApplicationForegrounded()
        underTest.onApplicationForegrounded()

        Mockito
            .verify(scheduler, Mockito.times(1))
            .scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS)
    }

    @Test
    fun testOnApplicationBackgrounded() {
        Mockito
            .doReturn(future)
            .`when`(scheduler)
            .scheduleWithFixedDelay(anrWatcher, 1, 1, TimeUnit.SECONDS)

        underTest.onApplicationForegrounded()

        underTest.onApplicationBackgrounded()
        underTest.onApplicationBackgrounded()
        underTest.onApplicationBackgrounded()

        Mockito.verify(future, Mockito.times(1)).cancel(true)
    }
}
