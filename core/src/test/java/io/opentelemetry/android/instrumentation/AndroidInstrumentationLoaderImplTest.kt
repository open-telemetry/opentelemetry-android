/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AndroidInstrumentationLoaderImplTest {
    private lateinit var loader: AndroidInstrumentationLoaderImpl

    @BeforeEach
    fun setUp() {
        loader = AndroidInstrumentationLoaderImpl()
    }

    @Test
    fun `Find implementations available in the classpath when querying an instrumentation`() {
        val instrumentation = loader.getByType(TestAndroidInstrumentation::class.java)

        assertThat(instrumentation).isNotNull()
        assertThat(checkNotNull(instrumentation).installed).isFalse()

        instrumentation.install(mockk())

        assertThat(checkNotNull(loader.getByType(TestAndroidInstrumentation::class.java)).installed).isTrue()
    }

    @Test
    fun `Find implementations available in the classpath when querying all instrumentations`() {
        val instrumentations = loader.getAll()

        assertThat(instrumentations).hasSize(1)
        assertThat(instrumentations.first()).isInstanceOf(TestAndroidInstrumentation::class.java)
    }
}
