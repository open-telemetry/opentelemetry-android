/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal

import android.content.Context
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.OrderedAndroidInstrumentation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InstrumentationSorterTest {
    private open class TestInstrumentation(
        override val name: String,
    ) : AndroidInstrumentation {
        override fun install(
            context: Context,
            openTelemetryRum: OpenTelemetryRum,
        ) {}
    }

    private class OrderedTestInstrumentation(
        name: String,
        override val installAfter: Set<String> = emptySet(),
        override val installBefore: Set<String> = emptySet(),
    ) : TestInstrumentation(name),
        OrderedAndroidInstrumentation

    @Test
    fun `empty list returns empty`() {
        val result = InstrumentationSorter.sort(emptyList())
        assertThat(result).isEmpty()
    }

    @Test
    fun `single item returns single item`() {
        val inst = TestInstrumentation("a")
        val result = InstrumentationSorter.sort(listOf(inst))
        assertThat(result).containsExactly(inst)
    }

    @Test
    fun `preserves discovery order when no ordering constraints are present`() {
        val a = TestInstrumentation("a")
        val b = TestInstrumentation("b")
        val c = TestInstrumentation("c")

        val result = InstrumentationSorter.sort(listOf(a, b, c))
        assertThat(result).containsExactly(a, b, c)
    }

    @Test
    fun `orders when installAfter is used`() {
        val b = OrderedTestInstrumentation("b", installAfter = setOf("a"))
        val a = TestInstrumentation("a")

        val result = InstrumentationSorter.sort(listOf(b, a))
        assertThat(result.map { it.name }).containsExactly("a", "b")
    }

    @Test
    fun `orders when installBefore is used`() {
        val b = TestInstrumentation("b")
        val a = OrderedTestInstrumentation("a", installBefore = setOf("b"))

        val result = InstrumentationSorter.sort(listOf(b, a))
        assertThat(result.map { it.name }).containsExactly("a", "b")
    }

    @Test
    fun `orders session before native crash and remaining maintain discovery order`() {
        val viewClick = TestInstrumentation("view-click")
        val nativeCrash = OrderedTestInstrumentation("native-crash", installAfter = setOf("session"))
        val anr = TestInstrumentation("anr")
        val session = OrderedTestInstrumentation("session", installBefore = setOf("native-crash"))

        val result = InstrumentationSorter.sort(listOf(viewClick, nativeCrash, anr, session))
        val names = result.map { it.name }

        assertThat(names.indexOf("session")).isLessThan(names.indexOf("native-crash"))
        // view-click and anr keep their relative discovery order
        assertThat(names.indexOf("view-click")).isLessThan(names.indexOf("anr"))
    }

    @Test
    fun `handles cycle gracefully by logging warning and preserving discovery order`() {
        val warnings = mutableListOf<String>()
        val a = OrderedTestInstrumentation("a", installAfter = setOf("b"))
        val b = OrderedTestInstrumentation("b", installAfter = setOf("a"))
        val c = TestInstrumentation("c")

        val result = InstrumentationSorter.sort(listOf(a, b, c)) { warnings.add(it) }

        assertThat(warnings).hasSize(1)
        assertThat(warnings[0]).contains("Cycle detected")
        // Everything still gets installed
        assertThat(result.map { it.name }).containsExactlyInAnyOrder("a", "b", "c")
    }

    @Test
    fun `ignores non-existent instrumentation in constraints`() {
        val a = OrderedTestInstrumentation("a", installAfter = setOf("non-existent"))
        val b = TestInstrumentation("b")

        val result = InstrumentationSorter.sort(listOf(a, b))
        assertThat(result.map { it.name }).containsExactly("a", "b")
    }
}
