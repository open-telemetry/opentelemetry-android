/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class OkHttpInstrumentationConfigTest {
    @Test
    void validateDefaultHttpMethods() {
        assertThat(OkHttpInstrumentationConfig.getKnownMethods())
                .containsExactlyInAnyOrder(
                        "CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT",
                        "TRACE");
    }
}
