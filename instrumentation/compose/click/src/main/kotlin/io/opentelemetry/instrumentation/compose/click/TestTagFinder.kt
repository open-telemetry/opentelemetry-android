package io.opentelemetry.instrumentation.compose.click

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kotlin.jvm.java
import kotlin.text.isNullOrEmpty

private const val testTagFieldName = "tag"

fun Modifier.getTestTag(): String? {
    return findTestTagInModifier(this)
}

internal fun findTestTagInModifier(modifier: Modifier): String? {
    var testTag = (modifier as? SemanticsModifier)?.semanticsConfiguration?.getOrNull(
        SemanticsProperties.TestTag
    )
    if (!testTag.isNullOrEmpty()) {
        return testTag
    }
    // Often the Modifier is a TestTagElement.
    if ("androidx.compose.ui.platform.TestTagElement" == modifier::class.qualifiedName) {
        try {
            val testTagField = modifier::class.java.getDeclaredField(testTagFieldName)
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