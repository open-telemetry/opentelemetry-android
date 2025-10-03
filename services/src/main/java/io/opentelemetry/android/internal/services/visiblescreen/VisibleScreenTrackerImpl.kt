/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.NavHost
import io.opentelemetry.android.internal.services.visiblescreen.activities.Pre29VisibleScreenLifecycleBinding
import io.opentelemetry.android.internal.services.visiblescreen.activities.VisibleScreenLifecycleBinding
import io.opentelemetry.android.internal.services.visiblescreen.fragments.RumFragmentActivityRegisterer
import io.opentelemetry.android.internal.services.visiblescreen.fragments.VisibleFragmentTracker
import java.util.concurrent.atomic.AtomicReference

/**
 * Wherein we do our best to figure out what "screen" is visible and what was the previously visible
 * "screen".
 *
 * In general, we favor using the last fragment that was resumed, but fall back to the last
 * resumed activity in case we don't have a fragment.
 *
 * We always ignore NavHostFragment instances since they aren't ever visible to the user.
 *
 * We have to treat DialogFragments slightly differently since they don't replace the launching
 * screen, and the launching screen never leaves visibility.
 */
internal class VisibleScreenTrackerImpl internal constructor(
    context: Context,
) : VisibleScreenTracker {
    private val lastResumedActivity = AtomicReference<String>()
    private val previouslyLastResumedActivity = AtomicReference<String>()
    private val lastResumedFragment = AtomicReference<String>()
    private val previouslyLastResumedFragment = AtomicReference<String?>()
    private val activityLifecycleTracker by lazy { buildActivitiesTracker() }
    private val fragmentLifecycleTrackerRegisterer by lazy { buildFragmentsTrackerRegisterer() }

    private val application = context as? Application

    init {
        application?.let {
            it.registerActivityLifecycleCallbacks(activityLifecycleTracker)
            it.registerActivityLifecycleCallbacks(fragmentLifecycleTrackerRegisterer)
        }
    }

    private fun buildActivitiesTracker(): Application.ActivityLifecycleCallbacks =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Pre29VisibleScreenLifecycleBinding(this)
        } else {
            VisibleScreenLifecycleBinding(this)
        }

    private fun buildFragmentsTrackerRegisterer(): Application.ActivityLifecycleCallbacks {
        val fragmentLifecycle = VisibleFragmentTracker(this)
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            RumFragmentActivityRegisterer.createPre29(fragmentLifecycle)
        } else {
            RumFragmentActivityRegisterer.create(fragmentLifecycle)
        }
    }

    override val previouslyVisibleScreen: String?
        get() {
            val previouslyLastFragment = previouslyLastResumedFragment.get()
            if (previouslyLastFragment != null) {
                return previouslyLastFragment
            }
            return previouslyLastResumedActivity.get()
        }

    override val currentlyVisibleScreen: String
        get() {
            val lastFragment = lastResumedFragment.get()
            if (lastFragment != null) {
                return lastFragment
            }
            val lastActivity = lastResumedActivity.get()
            if (lastActivity != null) {
                return lastActivity
            }
            return "unknown"
        }

    override fun activityResumed(activity: Activity) {
        lastResumedActivity.set(activity.javaClass.simpleName)
    }

    override fun activityPaused(activity: Activity) {
        previouslyLastResumedActivity.set(activity.javaClass.simpleName)
        lastResumedActivity.compareAndSet(activity.javaClass.simpleName, null)
    }

    override fun fragmentResumed(fragment: Fragment) {
        // skip the Fragment if it's a NavHost since it's never really "visible" by itself.
        if (fragment is NavHost) {
            return
        }

        if (fragment is DialogFragment) {
            previouslyLastResumedFragment.set(lastResumedFragment.get())
        }
        lastResumedFragment.set(fragment.javaClass.simpleName)
    }

    override fun fragmentPaused(fragment: Fragment) {
        // skip the Fragment if it's a NavHost since it's never really "visible" by itself.
        if (fragment is NavHost) {
            return
        }
        if (fragment is DialogFragment) {
            lastResumedFragment.set(previouslyLastResumedFragment.get())
        } else {
            lastResumedFragment.compareAndSet(fragment.javaClass.simpleName, null)
        }
        previouslyLastResumedFragment.set(fragment.javaClass.simpleName)
    }

    override fun close() {
        application?.let {
            it.unregisterActivityLifecycleCallbacks(activityLifecycleTracker)
            it.unregisterActivityLifecycleCallbacks(fragmentLifecycleTrackerRegisterer)
        }
    }
}
