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
        override val installFirst: Boolean = false,
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

    @Test
    fun `default getters return empty sets for ordered instrumentation`() {
        val defaultOrdered =
            object : OrderedAndroidInstrumentation {
                override val name: String = "test-default"

                override fun install(
                    context: Context,
                    openTelemetryRum: OpenTelemetryRum,
                ) {}
            }

        assertThat(defaultOrdered.installAfter).isEmpty()
        assertThat(defaultOrdered.installBefore).isEmpty()
        assertThat(defaultOrdered.installFirst).isFalse()
    }

    @Test
    fun `preserves duplicate registrations in discovery order`() {
        val foo1 = TestInstrumentation("foo")
        val foo2 = TestInstrumentation("foo")
        val bar = TestInstrumentation("bar")

        val result = InstrumentationSorter.sort(listOf(foo1, bar, foo2))
        assertThat(result).containsExactly(foo1, bar, foo2)
    }

    @Test
    fun `installFirst places instrumentation before unconstrained ones`() {
        val startup = TestInstrumentation("startup")
        val session = OrderedTestInstrumentation("session", installFirst = true, installBefore = setOf("native-crash"))
        val nativeCrash = OrderedTestInstrumentation("native-crash", installAfter = setOf("session"))
        val anr = TestInstrumentation("anr")

        val result = InstrumentationSorter.sort(listOf(startup, nativeCrash, session, anr))
        val names = result.map { it.name }

        assertThat(names).containsExactly("session", "startup", "native-crash", "anr")
    }

    @Test
    fun `installFirst respects explicit installAfter without creating cycle`() {
        val warnings = mutableListOf<String>()
        val setup = TestInstrumentation("setup")
        val session =
            OrderedTestInstrumentation(
                "session",
                installFirst = true,
                installAfter = setOf("setup"),
                installBefore = setOf("native-crash"),
            )
        val nativeCrash = OrderedTestInstrumentation("native-crash", installAfter = setOf("session"))
        val unconstrained = TestInstrumentation("other")

        val result = InstrumentationSorter.sort(listOf(unconstrained, nativeCrash, session, setup)) { warnings.add(it) }
        val names = result.map { it.name }

        assertThat(warnings).isEmpty()
        // setup must precede session due to explicit installAfter
        assertThat(names.indexOf("setup")).isLessThan(names.indexOf("session"))
        // session must precede nativeCrash due to explicit installBefore
        assertThat(names.indexOf("session")).isLessThan(names.indexOf("native-crash"))
        // session has installFirst so it still precedes unconstrained 'other'
        assertThat(names.indexOf("session")).isLessThan(names.indexOf("other"))
    }
}
