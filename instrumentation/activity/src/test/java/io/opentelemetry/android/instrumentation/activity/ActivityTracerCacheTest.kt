/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.activity

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

internal class ActivityTracerCacheTest {
    private lateinit var activity: Activity
    private lateinit var activityTracer: ActivityTracer
    private lateinit var tracerCreator: Function<Activity, ActivityTracer>
    private lateinit var initialActivity: AtomicReference<String>

    @BeforeEach
    fun setup() {
        activity = mockk()
        activityTracer = mockk()
        tracerCreator = mockk()
        initialActivity = AtomicReference()
    }

    @Test
    fun addEventNewActivity() {
        every { tracerCreator.apply(activity) } returns activityTracer
        every { activityTracer.addEvent(any()) } returns activityTracer

        val underTest = ActivityTracerCache(tracerCreator)
        val result = underTest.addEvent(activity, "beep")

        assertThat(result).isSameAs(activityTracer)
        verify { activityTracer.addEvent("beep") }
    }

    @Test
    fun addEventExistingActivity() {
        every { tracerCreator.apply(activity) } returns activityTracer
        every { activityTracer.addEvent(any()) } returns activityTracer

        val underTest = ActivityTracerCache(tracerCreator)
        val result1 = underTest.addEvent(activity, "beep1")
        val result2 = underTest.addEvent(activity, "beep2")
        val result3 = underTest.addEvent(activity, "beep3")

        assertThat(result1).isSameAs(activityTracer)
        assertThat(result2).isSameAs(activityTracer)
        assertThat(result3).isSameAs(activityTracer)

        verify {
            activityTracer.addEvent("beep1")
            activityTracer.addEvent("beep2")
            activityTracer.addEvent("beep3")
            tracerCreator.apply(activity)
        }
    }

    @Test
    fun startSpanIfNoneInProgress() {
        every { tracerCreator.apply(activity) } returns activityTracer
        every { activityTracer.startSpanIfNoneInProgress("wrenchy") } returns activityTracer

        val underTest = ActivityTracerCache(tracerCreator)
        val result = underTest.startSpanIfNoneInProgress(activity, "wrenchy")

        assertThat(result).isSameAs(activityTracer)
        verify { activityTracer.startSpanIfNoneInProgress("wrenchy") }
    }

    @Test
    fun initiateRestartSpanIfNecessary_singleActivity() {
        every { tracerCreator.apply(activity) } returns activityTracer
        every { activityTracer.initiateRestartSpanIfNecessary(false) } returns activityTracer

        val underTest = ActivityTracerCache(tracerCreator)
        val result = underTest.initiateRestartSpanIfNecessary(activity)

        assertThat(result).isSameAs(activityTracer)
        verify(exactly = 1) { activityTracer.initiateRestartSpanIfNecessary(false) }
    }

    @Test
    fun initiateRestartSpanIfNecessary_multiActivity() {
        val activity2: Activity = object : Activity() {}
        val activityTracer2: ActivityTracer = mockk(relaxed = true)

        every { tracerCreator.apply(activity) } returns activityTracer
        every { tracerCreator.apply(activity2) } returns activityTracer2
        every { activityTracer.addEvent(any()) } returns activityTracer
        every { activityTracer.initiateRestartSpanIfNecessary(true) } returns activityTracer

        val underTest = ActivityTracerCache(tracerCreator)
        underTest.addEvent(activity, "foo")
        underTest.addEvent(activity2, "bar")
        val result = underTest.initiateRestartSpanIfNecessary(activity)

        assertThat(result).isSameAs(activityTracer)
        verify { activityTracer.initiateRestartSpanIfNecessary(true) }
    }

    @Test
    fun startActivityCreation() {
        every { tracerCreator.apply(activity) } returns activityTracer
        every { activityTracer.startActivityCreation() } returns activityTracer

        val underTest = ActivityTracerCache(tracerCreator)
        val result = underTest.startActivityCreation(activity)

        assertThat(result).isSameAs(activityTracer)
        verify { activityTracer.startActivityCreation() }
    }
}
