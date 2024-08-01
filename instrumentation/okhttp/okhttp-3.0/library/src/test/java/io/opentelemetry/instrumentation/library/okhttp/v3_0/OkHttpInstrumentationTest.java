/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.opentelemetry.android.OpenTelemetryRum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OkHttpInstrumentationTest {
    private OkHttpInstrumentation instrumentation;

    @BeforeEach
    void setUp() {
        instrumentation = new OkHttpInstrumentation();
    }

    @Test
    void validateDefaultHttpMethods() {
        assertThat(instrumentation.getKnownMethods())
                .containsExactlyInAnyOrder(
                        "CONNECT", "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT",
                        "TRACE");
    }

    @Test
    void validateDefaultRumInstance() {
        assertThat(instrumentation.getOpenTelemetryRum()).isEqualTo(OpenTelemetryRum.noop());
    }

    @Test
    void validateInstall() {
        OpenTelemetryRum rum = mock();

        instrumentation.install(mock(), rum);

        assertThat(instrumentation.getOpenTelemetryRum()).isEqualTo(rum);
    }
}
