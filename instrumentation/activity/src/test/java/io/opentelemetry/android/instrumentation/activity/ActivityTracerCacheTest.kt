/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

@ExtendWith(MockitoExtension::class)
internal class ActivityTracerCacheTest {
    @Mock
    private lateinit var activity: Activity

    @Mock
    private lateinit var activityTracer: ActivityTracer

    @Mock
    private lateinit var tracerCreator: Function<Activity, ActivityTracer>
    private lateinit var initialActivity: AtomicReference<String>

    @BeforeEach
    fun setup() {
        initialActivity = AtomicReference<String>()
    }

    @Test
    fun addEventNewActivity() {
        Mockito.`when`(tracerCreator.apply(activity)).thenReturn(activityTracer)
        Mockito
            .`when`(activityTracer.addEvent(ArgumentMatchers.anyString()))
            .thenReturn(activityTracer)

        val underTest = ActivityTracerCache(tracerCreator)
        val result = underTest.addEvent(activity, "beep")
        assertSame(activityTracer, result)
        Mockito.verify(activityTracer).addEvent("beep")
        Mockito.verifyNoMoreInteractions(tracerCreator)
    }

    @Test
    fun addEventExistingActivity() {
        Mockito
            .`when`(tracerCreator.apply(activity))
            .thenReturn(activityTracer)
        Mockito
            .`when`(activityTracer.addEvent(ArgumentMatchers.anyString()))
            .thenReturn(activityTracer)

        val underTest = ActivityTracerCache(tracerCreator)
        val result1 = underTest.addEvent(activity, "beep1")
        val result2 = underTest.addEvent(activity, "beep2")
        val result3 = underTest.addEvent(activity, "beep3")
        assertSame(activityTracer, result1)
        assertSame(activityTracer, result2)
        assertSame(activityTracer, result3)
        Mockito.verify(activityTracer).addEvent("beep1")
        Mockito.verify(activityTracer).addEvent("beep2")
        Mockito.verify(activityTracer).addEvent("beep3")
        Mockito.verify(tracerCreator).apply(activity)
    }

    @Test
    fun startSpanIfNoneInProgress() {
        Mockito.`when`(tracerCreator.apply(activity)).thenReturn(activityTracer)
        Mockito
            .`when`(activityTracer.startSpanIfNoneInProgress("wrenchy"))
            .thenReturn(activityTracer)

        val underTest = ActivityTracerCache(tracerCreator)

        val result = underTest.startSpanIfNoneInProgress(activity, "wrenchy")
        assertSame(activityTracer, result)
        Mockito.verify(activityTracer).startSpanIfNoneInProgress("wrenchy")
        Mockito.verifyNoMoreInteractions(tracerCreator)
    }

    @Test
    fun initiateRestartSpanIfNecessary_singleActivity() {
        Mockito.`when`(tracerCreator.apply(activity)).thenReturn(activityTracer)
        Mockito
            .`when`(activityTracer.initiateRestartSpanIfNecessary(false))
            .thenReturn(activityTracer)

        val underTest = ActivityTracerCache(tracerCreator)

        val result = underTest.initiateRestartSpanIfNecessary(activity)
        assertSame(activityTracer, result)
        Mockito.verify(activityTracer).initiateRestartSpanIfNecessary(false)
        Mockito.verifyNoMoreInteractions(tracerCreator)
    }

    @Test
    fun initiateRestartSpanIfNecessary_multiActivity() {
        val activity2: Activity =
            object : Activity() {
                // to get a new class name used in the cache
            }
        val activityTracer2 = Mockito.mock(ActivityTracer::class.java)

        Mockito.`when`(tracerCreator.apply(activity)).thenReturn(activityTracer)
        Mockito
            .`when`(tracerCreator.apply(activity2))
            .thenReturn(activityTracer2)
        Mockito
            .`when`(activityTracer.addEvent(ArgumentMatchers.anyString()))
            .thenReturn(activityTracer)
        Mockito
            .`when`(activityTracer.initiateRestartSpanIfNecessary(true))
            .thenReturn(activityTracer)

        val underTest = ActivityTracerCache(tracerCreator)

        underTest.addEvent(activity, "foo")
        underTest.addEvent(activity2, "bar")
        val result = underTest.initiateRestartSpanIfNecessary(activity)
        assertSame(activityTracer, result)
        Mockito.verify(activityTracer).initiateRestartSpanIfNecessary(true)
    }

    @Test
    fun startActivityCreation() {
        Mockito.`when`(tracerCreator.apply(activity)).thenReturn(activityTracer)
        Mockito
            .`when`(activityTracer.startActivityCreation())
            .thenReturn(activityTracer)

        val underTest = ActivityTracerCache(tracerCreator)

        val result = underTest.startActivityCreation(activity)
        assertSame(activityTracer, result)
        Mockito.verify(activityTracer).startActivityCreation()
    }
}
