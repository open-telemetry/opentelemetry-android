/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class OkHttpInstrumentationTest {
    @Test
    void validateDefaultHttpMethods() {
        assertThat(OkHttpInstrumentation.getKnownMethods())
                .containsExactlyInAnyOrder(
                        "CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT",
                        "TRACE");
    }
}
