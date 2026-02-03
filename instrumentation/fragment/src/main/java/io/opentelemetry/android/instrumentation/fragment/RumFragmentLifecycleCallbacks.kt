/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.opentelemetry.android.instrumentation.common.ActiveSpan
import io.opentelemetry.android.instrumentation.common.ScreenNameExtractor
import io.opentelemetry.api.trace.Tracer

internal class RumFragmentLifecycleCallbacks(
    private val tracer: Tracer,
    private val lastVisibleScreen: () -> String?,
    private val screenNameExtractor: ScreenNameExtractor,
) : FragmentManager.FragmentLifecycleCallbacks() {
    private val tracersByFragmentClassName: MutableMap<String, FragmentTracer> = mutableMapOf()

    override fun onFragmentPreAttached(
        fm: FragmentManager,
        f: Fragment,
        context: Context,
    ) {
        super.onFragmentPreAttached(fm, f, context)
        getTracer(f).startFragmentCreation().addEvent("fragmentPreAttached")
    }

    override fun onFragmentAttached(
        fm: FragmentManager,
        f: Fragment,
        context: Context,
    ) {
        super.onFragmentAttached(fm, f, context)
        addEvent(f, "fragmentAttached")
    }

    override fun onFragmentPreCreated(
        fm: FragmentManager,
        f: Fragment,
        savedInstanceState: Bundle?,
    ) {
        super.onFragmentPreCreated(fm, f, savedInstanceState)
        addEvent(f, "fragmentPreCreated")
    }

    override fun onFragmentCreated(
        fm: FragmentManager,
        f: Fragment,
        savedInstanceState: Bundle?,
    ) {
        super.onFragmentCreated(fm, f, savedInstanceState)
        addEvent(f, "fragmentCreated")
    }

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        super.onFragmentViewCreated(fm, f, v, savedInstanceState)
        getTracer(f).startSpanIfNoneInProgress("Restored").addEvent("fragmentViewCreated")
    }

    override fun onFragmentStarted(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentStarted(fm, f)
        addEvent(f, "fragmentStarted")
    }

    override fun onFragmentResumed(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentResumed(fm, f)
        getTracer(f)
            .startSpanIfNoneInProgress("Resumed")
            .addEvent("fragmentResumed")
            .addPreviousScreenAttribute()
            .endActiveSpan()
    }

    override fun onFragmentPaused(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentPaused(fm, f)
        getTracer(f).startSpanIfNoneInProgress("Paused").addEvent("fragmentPaused").endActiveSpan()
    }

    override fun onFragmentStopped(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentStopped(fm, f)
        getTracer(f)
            .startSpanIfNoneInProgress("Stopped")
            .addEvent("fragmentStopped")
            .endActiveSpan()
    }

    override fun onFragmentViewDestroyed(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentViewDestroyed(fm, f)
        getTracer(f)
            .startSpanIfNoneInProgress("ViewDestroyed")
            .addEvent("fragmentViewDestroyed")
            .endActiveSpan()
    }

    override fun onFragmentDestroyed(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentDestroyed(fm, f)
        // note: this might not get called if the dev has checked "retainInstance" on the fragment
        getTracer(f).startSpanIfNoneInProgress("Destroyed").addEvent("fragmentDestroyed")
    }

    override fun onFragmentDetached(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentDetached(fm, f)
        // this is a terminal operation, but might also be the only thing we see on app getting
        // killed, so
        getTracer(f)
            .startSpanIfNoneInProgress("Detached")
            .addEvent("fragmentDetached")
            .endActiveSpan()
    }

    private fun addEvent(
        fragment: Fragment,
        eventName: String,
    ) {
        val fragmentTracer = tracersByFragmentClassName[fragment.javaClass.name]
        fragmentTracer?.addEvent(eventName)
    }

    private fun getTracer(fragment: Fragment): FragmentTracer =
        tracersByFragmentClassName.getOrPut(fragment.javaClass.name) {
            FragmentTracer(
                fragment,
                tracer,
                screenNameExtractor.extract(fragment),
                ActiveSpan(lastVisibleScreen),
            )
        }
}
