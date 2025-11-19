package androidx.compose.ui.platform

import androidx.compose.ui.Modifier

// Test-only stand-in for the private Compose TestTagElement.
// Must have a private field named `tag` so reflection in the production code finds it.
class TestTagElement(private val tag: String) : Modifier.Element