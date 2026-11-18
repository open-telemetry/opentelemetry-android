/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.opentelemetry.instrumentation.compose.click

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull

internal class TestTagFinder {
    fun findTaggedTarget(node: LayoutNode): LayoutNode {
        var candidate = node
        while (true) {
            if (hasTag(candidate)) {
                return candidate
            }
            if (candidate.parent == null) {
                return node
            }
            candidate = candidate.parent!!
        }
    }

    fun findTag(node: LayoutNode): String? {
        for (info in node.getModifierInfo()) {
            val tag = findModifierTag(info.modifier)
            if (!tag.isNullOrEmpty()) {
                return tag
            }
        }
        return null
    }

    fun hasTag(node: LayoutNode): Boolean {
        for (info in node.getModifierInfo()) {
            if (hasModifierTag(info.modifier)) {
                return true
            }
        }
        return false
    }

    private fun findModifierTag(modifier: Modifier): String? {
        if (modifier is SemanticsModifier) {
            return modifier.semanticsConfiguration.getOrNull(SemanticsProperties.TestTag)
        }
        if (modifier::class.qualifiedName != TEST_TAG_ELEMENT_CLASS_NAME) {
            return null
        }
        // Modern Compose stores the tag in a private ModifierNodeElement.
        return try {
            val field = modifier::class.java.getDeclaredField(TEST_TAG_FIELD_NAME)
            field.isAccessible = true
            field.get(modifier) as String
        } catch (exception: ReflectiveOperationException) {
            Log.w(LOG_TAG, "Unable to read Compose test tag", exception)
            null
        } catch (exception: SecurityException) {
            Log.w(LOG_TAG, "Unable to access Compose test tag", exception)
            null
        }
    }

    private fun hasModifierTag(modifier: Modifier): Boolean {
        if (modifier is SemanticsModifier) {
            return modifier.semanticsConfiguration.getOrNull(SemanticsProperties.TestTag) != null
        }
        return modifier::class.qualifiedName == TEST_TAG_ELEMENT_CLASS_NAME
    }

    private companion object {
        const val LOG_TAG = "TestTagFinder"
        const val TEST_TAG_FIELD_NAME = "tag"
        const val TEST_TAG_ELEMENT_CLASS_NAME = "androidx.compose.ui.platform.TestTagElement"
    }
}
