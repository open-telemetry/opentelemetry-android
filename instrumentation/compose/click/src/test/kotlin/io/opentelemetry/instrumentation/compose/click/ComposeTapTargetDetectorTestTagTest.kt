/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ComposeTapTargetDetectorTestTagTest {
    private lateinit var detector: ComposeTapTargetDetector

    @MockK
    lateinit var composeLayoutNodeUtil: ComposeLayoutNodeUtil

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        detector = ComposeTapTargetDetector(composeLayoutNodeUtil, featureFlagUseTestTag = true)
    }

    @Test
    fun `name from testTag when feature flag enabled`() {
        val config = mockkClass(SemanticsConfiguration::class)
        every { config.getOrNull(eq(SemanticsProperties.TestTag)) } returns "submit_button"
        every { config.contains(eq(SemanticsActions.OnClick)) } returns true
        every { config.getOrNull(eq(SemanticsActions.OnClick)) } returns
            AccessibilityAction<() -> Boolean>("click") { true }

        val mod = mockkClass(SemanticsModifier::class)
        every { mod.semanticsConfiguration } returns config
        val info = mockkClass(ModifierInfo::class)
        every { info.modifier } returns mod

        val node = mockkClass(LayoutNode::class)
        every { node.getModifierInfo() } returns listOf(info)
        every { node.semanticsId } returns 100

        val name = detector.nodeToName(node)
        assertThat(name).isEqualTo("submit_button")
    }

    @Test
    fun `falls back to onClick label when no testTag`() {
        val config = mockkClass(SemanticsConfiguration::class)
        every { config.getOrNull(eq(SemanticsProperties.TestTag)) } returns null
        every { config.contains(eq(SemanticsActions.OnClick)) } returns true
        every { config.getOrNull(eq(SemanticsActions.OnClick)) } returns
            AccessibilityAction<() -> Boolean>("click") { true }
        every { config.getOrNull(eq(SemanticsProperties.ContentDescription)) } returns null

        val mod = mockkClass(SemanticsModifier::class)
        every { mod.semanticsConfiguration } returns config
        val info = mockkClass(ModifierInfo::class)
        every { info.modifier } returns mod

        val node = mockkClass(LayoutNode::class)
        every { node.getModifierInfo() } returns listOf(info)
        every { node.semanticsId } returns 100

        val name = detector.nodeToName(node)
        assertThat(name).isEqualTo("click")
    }

    @Test
    fun `falls back to content description when no testTag`() {
        val config = mockkClass(SemanticsConfiguration::class)
        every { config.getOrNull(eq(SemanticsProperties.TestTag)) } returns null
        every { config.contains(eq(SemanticsActions.OnClick)) } returns true
        every { config.getOrNull(eq(SemanticsActions.OnClick)) } returns null
        every { config.getOrNull(eq(SemanticsProperties.ContentDescription)) } returns listOf("clickMe")

        val mod = mockkClass(SemanticsModifier::class)
        every { mod.semanticsConfiguration } returns config
        val info = mockkClass(ModifierInfo::class)
        every { info.modifier } returns mod

        val node = mockkClass(LayoutNode::class)
        every { node.getModifierInfo() } returns listOf(info)
        every { node.semanticsId } returns 100

        val name = detector.nodeToName(node)
        assertThat(name).isEqualTo("clickMe")
    }

    @Test
    fun `testTag ignored when feature flag disabled`() {
        val detectorNoFlag = ComposeTapTargetDetector(composeLayoutNodeUtil)

        val config = mockkClass(SemanticsConfiguration::class)
        every { config.getOrNull(eq(SemanticsProperties.TestTag)) } returns "submit_button"
        every { config.contains(eq(SemanticsActions.OnClick)) } returns true
        every { config.getOrNull(eq(SemanticsActions.OnClick)) } returns
            AccessibilityAction<() -> Boolean>("click") { true }
        every { config.getOrNull(eq(SemanticsProperties.ContentDescription)) } returns null

        val mod = mockkClass(SemanticsModifier::class)
        every { mod.semanticsConfiguration } returns config
        val info = mockkClass(ModifierInfo::class)
        every { info.modifier } returns mod

        val node = mockkClass(LayoutNode::class)
        every { node.getModifierInfo() } returns listOf(info)
        every { node.semanticsId } returns 100

        val name = detectorNoFlag.nodeToName(node)
        assertThat(name).isEqualTo("click")
    }
}
