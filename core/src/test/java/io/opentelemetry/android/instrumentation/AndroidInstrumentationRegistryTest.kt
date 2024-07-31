/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidInstrumentationRegistryTest {
    @Test
    fun `Verify singleton`() {
        val registry = AndroidInstrumentationRegistry.get()

        assertThat(registry).isEqualTo(AndroidInstrumentationRegistry.get())
    }
}
