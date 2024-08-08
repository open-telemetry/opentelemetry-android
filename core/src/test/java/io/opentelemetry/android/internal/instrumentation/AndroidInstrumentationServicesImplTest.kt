/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.instrumentation

import android.app.Application
import io.mockk.mockk
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.TestAndroidInstrumentation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AndroidInstrumentationServicesImplTest {
    private lateinit var registry: AndroidInstrumentationServicesImpl

    @BeforeEach
    fun setUp() {
        registry = AndroidInstrumentationServicesImpl()
    }

    @Test
    fun `Find and register implementations available in the classpath when querying an instrumentation`() {
        val instrumentation = registry.getByType(TestAndroidInstrumentation::class.java)!!

        assertThat(instrumentation.installed).isFalse()

        instrumentation.install(mockk(), mockk())

        assertThat(registry.getByType(TestAndroidInstrumentation::class.java)!!.installed).isTrue()
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

        assertThat(registry.getByType(DummyInstrumentation::class.java)!!.name).isEqualTo("test")
    }

    @Test
    fun `Register only one instrumentation per type`() {
        val instrumentation = DummyInstrumentation("test")
        val instrumentation2 = DummyInstrumentation("test2")

        registry.register(instrumentation)

        assertThatThrownBy {
            registry.register(instrumentation2)
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Instrumentation with type '${DummyInstrumentation::class.java}' already exists.")
    }

    private class DummyInstrumentation(val name: String) : AndroidInstrumentation {
        override fun install(
            application: Application,
            openTelemetryRum: OpenTelemetryRum,
        ) {
        }
    }
}
