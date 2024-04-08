/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Build
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import io.opentelemetry.android.internal.services.Service
import io.opentelemetry.android.internal.services.visiblescreen.activities.Pre29VisibleScreenLifecycleBinding
import io.opentelemetry.android.internal.services.visiblescreen.activities.VisibleScreenLifecycleBinding
import io.opentelemetry.android.internal.services.visiblescreen.fragments.RumFragmentActivityRegisterer
import io.opentelemetry.android.internal.services.visiblescreen.fragments.VisibleFragmentTracker
import java.util.concurrent.atomic.AtomicReference

/**
 * Wherein we do our best to figure out what "screen" is visible and what was the previously visible
 * "screen".
 *
 * <p>In general, we favor using the last fragment that was resumed, but fall back to the last
 * resumed activity in case we don't have a fragment.
 *
 * <p>We always ignore NavHostFragment instances since they aren't ever visible to the user.
 *
 * <p>We have to treat DialogFragments slightly differently since they don't replace the launching
 * screen, and the launching screen never leaves visibility.
 */
class VisibleScreenService private constructor(private val application: Application) : Service {
    private val lastResumedActivity = AtomicReference<String?>()
    private val previouslyLastResumedActivity = AtomicReference<String>()
    private val lastResumedFragment = AtomicReference<String?>()
    private val previouslyLastResumedFragment = AtomicReference<String?>()
    private val activityLifecycleTracker by lazy { buildActivitiesTracker() }
    private val fragmentLifecycleTrackerRegisterer by lazy { buildFragmentsTrackerRegisterer() }

    companion object {
        @JvmStatic
        fun create(application: Application): VisibleScreenService {
            return VisibleScreenService(application)
        }
    }

    override fun start() {
        application.registerActivityLifecycleCallbacks(activityLifecycleTracker)
        application.registerActivityLifecycleCallbacks(fragmentLifecycleTrackerRegisterer)
    }

    override fun stop() {
        application.unregisterActivityLifecycleCallbacks(activityLifecycleTracker)
        application.unregisterActivityLifecycleCallbacks(fragmentLifecycleTrackerRegisterer)
    }

    private fun buildActivitiesTracker(): ActivityLifecycleCallbacks {
        return if (Build.VERSION.SDK_INT < 29) {
            Pre29VisibleScreenLifecycleBinding(this)
        } else {
            VisibleScreenLifecycleBinding(this)
        }
    }

    private fun buildFragmentsTrackerRegisterer(): ActivityLifecycleCallbacks {
        val fragmentLifecycle = VisibleFragmentTracker(this)
        return if (Build.VERSION.SDK_INT < 29) {
            RumFragmentActivityRegisterer.createPre29(fragmentLifecycle)
        } else {
            RumFragmentActivityRegisterer.create(fragmentLifecycle)
        }
    }

    fun getPreviouslyVisibleScreen(): String? {
        val previouslyLastFragment = previouslyLastResumedFragment.get()
        return previouslyLastFragment ?: previouslyLastResumedActivity.get()
    }

    fun getCurrentlyVisibleScreen(): String {
        val lastFragment = lastResumedFragment.get()
        if (lastFragment != null) {
            return lastFragment
        }
        val lastActivity = lastResumedActivity.get()
        return lastActivity ?: "unknown"
    }

    fun activityResumed(activity: Activity) {
        lastResumedActivity.set(activity.javaClass.getSimpleName())
    }

    fun activityPaused(activity: Activity) {
        previouslyLastResumedActivity.set(activity.javaClass.getSimpleName())
        lastResumedActivity.compareAndSet(activity.javaClass.getSimpleName(), null)
    }

    fun fragmentResumed(fragment: Fragment) {
        // skip the NavHostFragment since it's never really "visible" by itself.
        if (fragment is NavHostFragment) {
            return
        }
        if (fragment is DialogFragment) {
            previouslyLastResumedFragment.set(lastResumedFragment.get())
        }
        lastResumedFragment.set(fragment.javaClass.getSimpleName())
    }

    fun fragmentPaused(fragment: Fragment) {
        // skip the NavHostFragment since it's never really "visible" by itself.
        if (fragment is NavHostFragment) {
            return
        }
        if (fragment is DialogFragment) {
            lastResumedFragment.set(previouslyLastResumedFragment.get())
        } else {
            lastResumedFragment.compareAndSet(fragment.javaClass.getSimpleName(), null)
        }
        previouslyLastResumedFragment.set(fragment.javaClass.getSimpleName())
    }
}
