/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidInstrumentationLoaderTest {
    @Test
    fun `Verify singleton`() {
        val registry = AndroidInstrumentationLoader.get()

        assertThat(registry).isEqualTo(AndroidInstrumentationLoader.get())
    }
}
