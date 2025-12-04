/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.click

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.TestTagElement
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.jupiter.api.assertNull

class TestTagFinderTest {
    @Test
    fun `finds tag via reflection on TestTagElement`() {
        val element = TestTagElement("my-test-tag")
        val result = findTestTagInModifier(element as Modifier)
        assertEquals("my-test-tag", result)
    }

    @Test
    fun `finds tag via reflection on TestTagElement with empty value`() {
        val element = TestTagElement("")
        val result = findTestTagInModifier(element as Modifier)
        assertNull(result)
    }

    @Test
    fun `finds tag via reflection on Modifier`() {
        val element = Modifier
        val result = findTestTagInModifier(element)
        assertNull(result)
    }
}
