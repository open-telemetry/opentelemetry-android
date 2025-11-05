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
import io.mockk.mockk

internal class FragmentCallbackTestHarness(
    private val callbacks: RumFragmentLifecycleCallbacks,
) {
    fun runFragmentCreationLifecycle(fragment: Fragment) {
        val context = mockk<Context>()
        val fragmentManager = mockk<FragmentManager>()
        val bundle = mockk<Bundle>()

        callbacks.onFragmentPreAttached(fragmentManager, fragment, context)
        callbacks.onFragmentAttached(fragmentManager, fragment, context)
        callbacks.onFragmentPreCreated(fragmentManager, fragment, bundle)
        callbacks.onFragmentCreated(fragmentManager, fragment, bundle)
        runFragmentRestoredLifecycle(fragment)
    }

    fun runFragmentRestoredLifecycle(fragment: Fragment) {
        val fragmentManager = mockk<FragmentManager>()
        val bundle = mockk<Bundle>()
        val view = mockk<View>()
        callbacks.onFragmentViewCreated(fragmentManager, fragment, view, bundle)
        callbacks.onFragmentStarted(fragmentManager, fragment)
        callbacks.onFragmentResumed(fragmentManager, fragment)
    }

    fun runFragmentResumedLifecycle(fragment: Fragment) {
        val fragmentManager = mockk<FragmentManager>()
        callbacks.onFragmentResumed(fragmentManager, fragment)
    }

    fun runFragmentPausedLifecycle(fragment: Fragment) {
        val fragmentManager = mockk<FragmentManager>()
        callbacks.onFragmentPaused(fragmentManager, fragment)
        callbacks.onFragmentStopped(fragmentManager, fragment)
    }

    fun runFragmentDetachedFromActiveLifecycle(fragment: Fragment) {
        val fragmentManager = mockk<FragmentManager>()

        runFragmentPausedLifecycle(fragment)
        callbacks.onFragmentViewDestroyed(fragmentManager, fragment)
        callbacks.onFragmentDestroyed(fragmentManager, fragment)
        runFragmentDetachedLifecycle(fragment)
    }

    fun runFragmentViewDestroyedFromStoppedLifecycle(fragment: Fragment) {
        val fragmentManager = mockk<FragmentManager>()

        callbacks.onFragmentViewDestroyed(fragmentManager, fragment)
    }

    fun runFragmentDetachedFromStoppedLifecycle(fragment: Fragment) {
        val fragmentManager = mockk<FragmentManager>()

        runFragmentViewDestroyedFromStoppedLifecycle(fragment)
        callbacks.onFragmentDestroyed(fragmentManager, fragment)
        runFragmentDetachedLifecycle(fragment)
    }

    fun runFragmentDetachedLifecycle(fragment: Fragment) {
        val fragmentManager = mockk<FragmentManager>()

        callbacks.onFragmentDetached(fragmentManager, fragment)
    }
}
