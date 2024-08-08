/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.instrumentation

import io.mockk.mockk
import io.opentelemetry.android.instrumentation.TestAndroidInstrumentation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AndroidInstrumentationServicesImplTest {
    private lateinit var registry: AndroidInstrumentationServicesImpl

    @BeforeEach
    fun setUp() {
        registry = AndroidInstrumentationServicesImpl()
    }

    @Test
    fun `Find implementations available in the classpath when querying an instrumentation`() {
        val instrumentation = registry.getByType(TestAndroidInstrumentation::class.java)!!

        assertThat(instrumentation.installed).isFalse()

        instrumentation.install(mockk(), mockk())

        assertThat(registry.getByType(TestAndroidInstrumentation::class.java)!!.installed).isTrue()
    }

    @Test
    fun `Find implementations available in the classpath when querying all instrumentations`() {
        val instrumentations = registry.getAll()

        assertThat(instrumentations).hasSize(1)
        assertThat(instrumentations.first()).isInstanceOf(TestAndroidInstrumentation::class.java)
    }
}
