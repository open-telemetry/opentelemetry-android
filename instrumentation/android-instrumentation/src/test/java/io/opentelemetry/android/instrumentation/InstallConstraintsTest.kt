/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class InstallConstraintsTest {
    private fun instrumentation(name: String): AndroidInstrumentation =
        mockk {
            every { this@mockk.name } returns name
        }

    private val foo = instrumentation("foo")
    private val bar = instrumentation("bar")

    @Test
    fun `no constraints have no preference`() {
        assertThat(InstallConstraints().compareTo(foo)).isZero()
    }

    @Test
    fun `before and after apply only to named instrumentations`() {
        val constraints = InstallConstraints(installBefore = setOf("foo"), installAfter = setOf("bar"))

        assertThat(constraints.compareTo(foo)).isNegative()
        assertThat(constraints.compareTo(bar)).isPositive()
        assertThat(constraints.compareTo(instrumentation("baz"))).isZero()
    }

    @Test
    fun `first and last apply to all instrumentations`() {
        assertThat(InstallConstraints(installFirst = true).compareTo(foo)).isNegative()
        assertThat(InstallConstraints(installLast = true).compareTo(foo)).isPositive()
    }

    @Test
    fun `named constraints take precedence over first and last`() {
        assertThat(InstallConstraints(installFirst = true, installAfter = setOf("foo")).compareTo(foo)).isPositive()
        assertThat(InstallConstraints(installLast = true, installBefore = setOf("foo")).compareTo(foo)).isNegative()
    }

    @Test
    fun `cannot install both first and last`() {
        assertThatThrownBy { InstallConstraints(installFirst = true, installLast = true) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `cannot install both before and after the same instrumentation`() {
        assertThatThrownBy { InstallConstraints(installBefore = setOf("foo"), installAfter = setOf("foo")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("foo")
    }
}
