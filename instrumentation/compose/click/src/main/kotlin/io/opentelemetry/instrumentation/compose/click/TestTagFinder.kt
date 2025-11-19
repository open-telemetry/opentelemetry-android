/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.click

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kotlin.jvm.java
import kotlin.text.isNullOrEmpty

private const val TEST_TAG_FIELD_NAME = "tag"

internal fun Modifier.getTestTag(): String? = findTestTagInModifier(this)

internal fun findTestTagInModifier(modifier: Modifier): String? {
    var testTag =
        (modifier as? SemanticsModifier)?.semanticsConfiguration?.getOrNull(
            SemanticsProperties.TestTag,
        )
    if (!testTag.isNullOrEmpty()) {
        return testTag
    }
    // Often the Modifier is a TestTagElement. As this class is private there is only a way to
    // get the TestTag value using reflection
    if ("androidx.compose.ui.platform.TestTagElement" == modifier::class.qualifiedName) {
        try {
            val testTagField = modifier::class.java.getDeclaredField(TEST_TAG_FIELD_NAME)
            testTagField.isAccessible = true
            testTag = testTagField.get(modifier) as? String
            if (!testTag.isNullOrEmpty()) {
                return testTag
            }
        } catch (_: Exception) {
        }
    }
    return null
}
