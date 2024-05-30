/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.app.Application
import io.mockk.mockk
import io.opentelemetry.android.OpenTelemetryRum
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class AndroidInstrumentationRegistryImplTest {
    private lateinit var registry: AndroidInstrumentationRegistryImpl

    @BeforeEach
    fun setUp() {
        registry = AndroidInstrumentationRegistryImpl()
    }

    @Test
    fun `Find and register implementations available in the classpath when querying an instrumentation`() {
        val instrumentation = registry.get(TestAndroidInstrumentation::class.java)

        instrumentation?.install(mockk(), mockk())

        assertThat(registry.get(TestAndroidInstrumentation::class.java)?.installed).isTrue()
    }

    @Test
    fun `Find and register implementations available in the classpath when querying all instrumentations`() {
        val instrumentations = registry.getAll()

        assertThat(instrumentations).hasSize(1)
        assertThat(instrumentations.first()).isInstanceOf(TestAndroidInstrumentation::class.java)
    }

    @Test
    fun `Register instrumentations`() {
        val instrumentation = DummyInstrumentation("test")

        registry.register(instrumentation)

        assertThat(registry.get(DummyInstrumentation::class.java)?.name).isEqualTo("test")
    }

    @Test
    fun `Register only one instrumentation per type`() {
        val instrumentation = DummyInstrumentation("test")
        val instrumentation2 = DummyInstrumentation("test2")

        registry.register(instrumentation)

        try {
            registry.register(instrumentation2)
            fail("Should not allow to register the same type of instrumentation more than once.")
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo("Instrumentation with type '${DummyInstrumentation::class.java}' already exists.")
        }
    }

    private class DummyInstrumentation(val name: String) : AndroidInstrumentation {
        override fun install(
            application: Application,
            openTelemetryRum: OpenTelemetryRum,
        ) {
        }
    }
}
