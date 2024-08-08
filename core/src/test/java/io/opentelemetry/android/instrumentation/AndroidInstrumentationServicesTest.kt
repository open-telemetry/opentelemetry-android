/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidInstrumentationServicesTest {
    @Test
    fun `Verify singleton`() {
        val registry = AndroidInstrumentationServices.get()

        assertThat(registry).isEqualTo(AndroidInstrumentationServices.get())
    }
}
