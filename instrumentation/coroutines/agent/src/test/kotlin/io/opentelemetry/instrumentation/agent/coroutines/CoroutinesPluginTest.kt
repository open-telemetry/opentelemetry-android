/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.coroutines

import net.bytebuddy.build.AndroidDescriptor
import net.bytebuddy.description.type.TypeDescription
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CoroutinesPluginTest {
    private val sampleType: TypeDescription = TypeDescription.ForLoadedType.of(Any::class.java)

    @Test
    fun `LOCAL scope matches`() {
        val plugin = CoroutinesPlugin(AndroidDescriptor.Trivial.LOCAL)
        assertThat(plugin.matches(sampleType)).isTrue()
    }

    @Test
    fun `EXTERNAL scope does not match`() {
        val plugin = CoroutinesPlugin(AndroidDescriptor.Trivial.EXTERNAL)
        assertThat(plugin.matches(sampleType)).isFalse()
    }
}
