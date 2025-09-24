/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.visiblescreen.fragments

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
internal class RumFragmentActivityRegistererTest {
    @Mock
    private lateinit var fragmentCallbacks: FragmentManager.FragmentLifecycleCallbacks

    @Test
    fun createHappyPath() {
        val activity = Mockito.mock(FragmentActivity::class.java)
        val manager = Mockito.mock(FragmentManager::class.java)

        Mockito.`when`(activity.supportFragmentManager).thenReturn(manager)

        val underTest =
            RumFragmentActivityRegisterer.create(fragmentCallbacks)

        underTest.onActivityPreCreated(activity, null)
        Mockito.verify(manager).registerFragmentLifecycleCallbacks(
            fragmentCallbacks,
            true,
        )
    }

    @Test
    fun callbackIgnoresNonFragmentActivity() {
        val activity = Mockito.mock(Activity::class.java)

        val underTest =
            RumFragmentActivityRegisterer.create(fragmentCallbacks)

        underTest.onActivityPreCreated(activity, null)
    }

    @Test
    fun createPre29HappyPath() {
        val activity = Mockito.mock(FragmentActivity::class.java)
        val manager = Mockito.mock(FragmentManager::class.java)

        Mockito.`when`(activity.supportFragmentManager).thenReturn(manager)

        val underTest =
            RumFragmentActivityRegisterer.createPre29(fragmentCallbacks)

        underTest.onActivityCreated(activity, null)
        Mockito.verify(manager).registerFragmentLifecycleCallbacks(
            fragmentCallbacks,
            true,
        )
    }

    @Test
    fun pre29CallbackIgnoresNonFragmentActivity() {
        val activity = Mockito.mock(Activity::class.java)

        val underTest =
            RumFragmentActivityRegisterer.createPre29(fragmentCallbacks)

        underTest.onActivityCreated(activity, null)
    }
}
