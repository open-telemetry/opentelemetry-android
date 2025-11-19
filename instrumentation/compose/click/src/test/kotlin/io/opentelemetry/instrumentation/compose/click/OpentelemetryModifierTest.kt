package io.opentelemetry.instrumentation.compose.click

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsModifier
import androidx.compose.ui.semantics.getOrNull
import org.junit.Assert.*
import org.junit.Test

class OpentelemetryModifierTest {

    @Test
    fun opentelemetryModifier_returnsModifiedModifier() {
        val modifier = Modifier.opentelemetry("custom name")
        with((modifier as SemanticsModifier).semanticsConfiguration) {
            val opentelemetryModifierValue = getOrNull(OpentelemetrySemanticsPropertyKey)
            assertEquals("custom name", opentelemetryModifierValue)
        }

    }
}
