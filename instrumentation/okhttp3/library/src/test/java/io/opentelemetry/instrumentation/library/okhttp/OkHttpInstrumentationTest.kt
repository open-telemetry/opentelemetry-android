/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp

import io.opentelemetry.instrumentation.library.okhttp.v3_0.OkHttpInstrumentation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OkHttpInstrumentationTest {
    @Test
    fun validateDefaultHttpMethods() {
        val instrumentation = OkHttpInstrumentation()
        assertThat(instrumentation.knownMethods)
            .containsExactlyInAnyOrder(
                "CONNECT",
                "DELETE",
                "GET",
                "HEAD",
                "OPTIONS",
                "PATCH",
                "POST",
                "PUT",
                "TRACE",
            )
    }
}
