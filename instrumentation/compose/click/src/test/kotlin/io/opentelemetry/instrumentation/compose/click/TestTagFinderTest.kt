/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsConfiguration
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockkClass
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestTagFinderTest {
    private val finder = TestTagFinder()

    @Test
    fun `reads tag from modern Compose modifier`() {
        val node: LayoutNode = nodeWithModifiers(Modifier.testTag("mb_button"))

        assertThat(finder.findTag(node)).isEqualTo("mb_button")
    }

    @Test
    fun `reads tag from legacy semantics modifier`() {
        val configuration = SemanticsConfiguration()
        configuration[SemanticsProperties.TestTag] = "legacy_button"
        val modifier = mockkClass(SemanticsModifier::class)
        every { modifier.semanticsConfiguration } returns configuration
        val node: LayoutNode = nodeWithModifiers(modifier)

        assertThat(finder.findTag(node)).isEqualTo("legacy_button")
    }

    @Test
    fun `ignores empty tags and returns first non-empty tag without concatenation`() {
        val node: LayoutNode =
            nodeWithModifiers(
                Modifier.testTag(""),
                Modifier.testTag("mb_button_label"),
                Modifier.testTag("mb_text"),
            )

        assertThat(finder.findTag(node)).isEqualTo("mb_button_label")
    }

    @Test
    fun `returns nearest tagged ancestor`() {
        val card: LayoutNode = nodeWithModifiers(Modifier.testTag("mb_card"))
        val button: LayoutNode = nodeWithModifiers(Modifier.testTag("mb_button"))
        val clickable: LayoutNode = nodeWithModifiers(Modifier)
        every { button.parent } returns card
        every { clickable.parent } returns button

        assertThat(finder.findTaggedTarget(clickable)).isEqualTo(button)
    }

    @Test
    fun `prefers own tag over ancestor tag`() {
        val card: LayoutNode = nodeWithModifiers(Modifier.testTag("mb_card"))
        val button: LayoutNode = nodeWithModifiers(Modifier.testTag("mb_button"))
        every { button.parent } returns card

        assertThat(finder.findTaggedTarget(button)).isEqualTo(button)
    }

    @Test
    fun `returns original target when ancestors have no tag`() {
        val parent: LayoutNode = nodeWithModifiers(Modifier)
        val clickable: LayoutNode = nodeWithModifiers(Modifier)
        every { clickable.parent } returns parent

        assertThat(finder.findTaggedTarget(clickable)).isEqualTo(clickable)
        assertThat(finder.findTag(clickable)).isNull()
    }

    private fun nodeWithModifiers(vararg modifiers: Modifier): LayoutNode {
        val node = mockkClass(LayoutNode::class)
        every { node.parent } returns null
        val infos =
            modifiers.map { modifier ->
                val info = mockkClass(ModifierInfo::class)
                every { info.modifier } returns modifier
                info
            }
        every { node.getModifierInfo() } returns infos
        return node
    }
}
