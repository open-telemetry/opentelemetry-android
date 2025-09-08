/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("DEPRECATION") // suppress deprecation for android.app.Fragment

package io.opentelemetry.android.instrumentation.common

import android.app.Activity
import android.app.Fragment
import io.opentelemetry.android.instrumentation.annotations.RumScreenName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ScreenNameExtractorTest {
    @Test
    fun testActivity() {
        val activity = Activity()
        val name = DefaultScreenNameExtractor.extract(activity)
        assertEquals("Activity", name)
    }

    @Test
    fun testFragment() {
        val fragment = Fragment()
        val name = DefaultScreenNameExtractor.extract(fragment)
        assertEquals("Fragment", name)
    }

    @Test
    fun testAndroidxFragment() {
        val fragment = androidx.fragment.app.Fragment()
        val name = DefaultScreenNameExtractor.extract(fragment)
        assertEquals("Fragment", name)
    }

    @Test
    fun testObject() {
        val obj = Object()
        val name = DefaultScreenNameExtractor.extract(obj)
        assertEquals("Object", name)
    }

    @Test
    fun testAnnotatedActivity() {
        val activity: Activity = AnnotatedActivity()
        val name = DefaultScreenNameExtractor.extract(activity)
        assertEquals("squarely", name)
    }

    @Test
    fun testAnnotatedFragment() {
        val fragment = AnnotatedFragment()
        val name = DefaultScreenNameExtractor.extract(fragment)
        assertEquals("bumpity", name)
    }

    @Test
    fun testAnnotatedAndroidxFragment() {
        val fragment = AnnotatedAndroidxFragment()
        val name = DefaultScreenNameExtractor.extract(fragment)
        assertEquals("xtra", name)
    }

    @Test
    fun testAnnotatedObject() {
        val obj = AnnotatedObject()
        val name = DefaultScreenNameExtractor.extract(obj)
        assertEquals("woohoo", name)
    }

    @RumScreenName("bumpity")
    private class AnnotatedFragment : Fragment()

    @RumScreenName("xtra")
    private class AnnotatedAndroidxFragment : androidx.fragment.app.Fragment()

    @RumScreenName("squarely")
    private class AnnotatedActivity : Activity()

    @RumScreenName("woohoo")
    private class AnnotatedObject
}
