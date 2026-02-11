/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.fragments

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.opentelemetry.android.internal.services.visiblescreen.activities.DefaultingActivityLifecycleCallbacks

/**
 * Registers the RumFragmentLifecycleCallbacks when an activity is created. There are just 2 factory
 * methods here, one for API level before 29, and one for the rest.
 */
object RumFragmentActivityRegisterer {
    fun create(fragmentCallbacks: FragmentManager.FragmentLifecycleCallbacks): ActivityLifecycleCallbacks =
        object : DefaultingActivityLifecycleCallbacks {
            override fun onActivityPreCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) {
                if (activity is FragmentActivity) {
                    register(activity, fragmentCallbacks)
                }
            }
        }

    fun createPre29(fragmentCallbacks: FragmentManager.FragmentLifecycleCallbacks): ActivityLifecycleCallbacks =
        object : DefaultingActivityLifecycleCallbacks {
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) {
                if (activity is FragmentActivity) {
                    register(activity, fragmentCallbacks)
                }
            }
        }

    private fun register(
        activity: FragmentActivity,
        fragmentCallbacks: FragmentManager.FragmentLifecycleCallbacks,
    ) {
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentCallbacks, true)
    }
}
