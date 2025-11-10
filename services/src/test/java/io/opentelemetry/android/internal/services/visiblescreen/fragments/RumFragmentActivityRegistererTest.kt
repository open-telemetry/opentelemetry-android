/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.fragments

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class RumFragmentActivityRegistererTest {
    @MockK
    private lateinit var fragmentCallbacks: FragmentManager.FragmentLifecycleCallbacks

    @Test
    fun createHappyPath() {
        val activity = mockk<FragmentActivity>()
        val manager = mockk<FragmentManager>(relaxed = true)

        every { activity.supportFragmentManager } returns manager

        val underTest =
            RumFragmentActivityRegisterer.create(fragmentCallbacks)

        underTest.onActivityPreCreated(activity, null)
        verify {
            manager.registerFragmentLifecycleCallbacks(
                fragmentCallbacks,
                true,
            )
        }
    }

    @Test
    fun callbackIgnoresNonFragmentActivity() {
        val activity = mockk<Activity>()

        val underTest =
            RumFragmentActivityRegisterer.create(fragmentCallbacks)

        underTest.onActivityPreCreated(activity, null)
    }

    @Test
    fun createPre29HappyPath() {
        val activity = mockk<FragmentActivity>()
        val manager = mockk<FragmentManager>(relaxed = true)

        every { activity.supportFragmentManager } returns manager

        val underTest =
            RumFragmentActivityRegisterer.createPre29(fragmentCallbacks)

        underTest.onActivityCreated(activity, null)
        verify {
            manager.registerFragmentLifecycleCallbacks(
                fragmentCallbacks,
                true,
            )
        }
    }

    @Test
    fun pre29CallbackIgnoresNonFragmentActivity() {
        val activity = mockk<Activity>()

        val underTest =
            RumFragmentActivityRegisterer.createPre29(fragmentCallbacks)

        underTest.onActivityCreated(activity, null)
    }
}
