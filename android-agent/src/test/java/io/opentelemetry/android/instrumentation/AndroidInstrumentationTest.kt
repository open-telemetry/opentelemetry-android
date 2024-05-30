/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidInstrumentationTest {
    @Test
    fun `Find and register implementations available in the classpath when querying an instrumentation`() {
        val instrumentation = AndroidInstrumentation.get(TestAndroidInstrumentation::class.java)

        instrumentation.install(mockk(), mockk())

        assertThat(AndroidInstrumentation.get(TestAndroidInstrumentation::class.java).installed).isTrue()
    }

    @Test
    fun `Find and register implementations available in the classpath when querying all instrumentations`() {
        val instrumentations = AndroidInstrumentation.getAll()

        assertThat(instrumentations).hasSize(1)
        assertThat(instrumentations.first()).isInstanceOf(TestAndroidInstrumentation::class.java)
    }
}
