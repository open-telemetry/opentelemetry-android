/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService

class VisibleFragmentTracker(private val visibleScreenService: VisibleScreenService) :
    FragmentManager.FragmentLifecycleCallbacks() {
    override fun onFragmentResumed(
        fm: FragmentManager,
        f: Fragment,
    ) {
        visibleScreenService.fragmentResumed(f)
    }

    override fun onFragmentPaused(
        fm: FragmentManager,
        f: Fragment,
    ) {
        super.onFragmentPaused(fm, f)
        visibleScreenService.fragmentPaused(f)
    }
}
