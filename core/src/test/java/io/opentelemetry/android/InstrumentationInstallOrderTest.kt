/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallConstraints
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class FakeInstrumentation(
    override val name: String,
    constraints: InstallConstraints = InstallConstraints(),
) : AndroidInstrumentation,
    Comparable<AndroidInstrumentation> by constraints {
    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
    }

    override fun toString(): String = name
}

private class PlainInstrumentation(
    override val name: String,
) : AndroidInstrumentation {
    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
    }

    override fun toString(): String = name
}

private class StringComparableInstrumentation(
    override val name: String,
) : AndroidInstrumentation,
    Comparable<String> {
    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
    }

    override fun compareTo(other: String): Int = -1

    override fun toString(): String = name
}

class InstrumentationInstallOrderTest {
    private val warnings = mutableListOf<String>()

    private fun sort(vararg instrumentations: AndroidInstrumentation): List<AndroidInstrumentation> =
        instrumentations.toList().sortedForInstall { warnings.add(it) }

    @Test
    fun `instrumentations without preferences keep their order`() {
        val a = PlainInstrumentation("a")
        val b = FakeInstrumentation("b")
        val c = PlainInstrumentation("c")

        assertThat(sort(a, b, c)).containsExactly(a, b, c)
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `first and last are placed around instrumentations without preferences`() {
        val a = PlainInstrumentation("a")
        val last = FakeInstrumentation("last", InstallConstraints(installLast = true))
        val b = PlainInstrumentation("b")
        val first = FakeInstrumentation("first", InstallConstraints(installFirst = true))

        assertThat(sort(a, last, b, first)).containsExactly(first, a, b, last)
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `before and after can target instrumentations without preferences`() {
        val a = PlainInstrumentation("a")
        val b = PlainInstrumentation("b")
        val beforeB = FakeInstrumentation("beforeB", InstallConstraints(installBefore = setOf("b")))
        val afterA = FakeInstrumentation("afterA", InstallConstraints(installAfter = setOf("a")))

        assertThat(sort(afterA, a, b, beforeB)).containsExactly(a, afterA, beforeB, b)
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `named constraints take precedence over first`() {
        val foo = PlainInstrumentation("foo")
        val nativeCrash =
            FakeInstrumentation(
                "native-crash",
                InstallConstraints(installFirst = true, installAfter = setOf("session")),
            )
        val session = FakeInstrumentation("session", InstallConstraints(installFirst = true))

        assertThat(sort(foo, nativeCrash, session)).containsExactly(session, nativeCrash, foo)
        assertThat(warnings).isEmpty()
    }

    @Test
    fun `competing first instrumentations keep their order`() {
        val a = PlainInstrumentation("a")
        val first1 = FakeInstrumentation("first1", InstallConstraints(installFirst = true))
        val first2 = FakeInstrumentation("first2", InstallConstraints(installFirst = true))

        assertThat(sort(a, first2, first1)).containsExactly(first2, first1, a)
        assertThat(warnings).hasSize(1)
        assertThat(warnings.single()).contains("first2", "first1")
    }

    @Test
    fun `contradictory named constraints keep their order`() {
        val a = FakeInstrumentation("a", InstallConstraints(installBefore = setOf("b")))
        val b = FakeInstrumentation("b", InstallConstraints(installBefore = setOf("a")))

        assertThat(sort(b, a)).containsExactly(b, a)
        assertThat(warnings).hasSize(1)
    }

    @Test
    fun `cycles are broken without losing instrumentations`() {
        val a = FakeInstrumentation("a", InstallConstraints(installBefore = setOf("b")))
        val b = FakeInstrumentation("b", InstallConstraints(installBefore = setOf("c")))
        val c = FakeInstrumentation("c", InstallConstraints(installBefore = setOf("a")))
        val d = PlainInstrumentation("d")

        val result = sort(d, c, b, a)

        assertThat(result).containsExactlyInAnyOrder(a, b, c, d)
        assertThat(warnings).anyMatch { it.contains("cycle") }
    }

    @Test
    fun `comparable of another type is treated as having no preference`() {
        val a = PlainInstrumentation("a")
        val other = StringComparableInstrumentation("other")
        val first = FakeInstrumentation("first", InstallConstraints(installFirst = true))

        assertThat(sort(a, other, first)).containsExactly(first, a, other)
        assertThat(warnings).isEmpty()
    }
}
