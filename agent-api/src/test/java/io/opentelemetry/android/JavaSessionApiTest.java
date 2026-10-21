/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.Clock;
import org.junit.jupiter.api.Test;

class JavaSessionApiTest {
    @Test
    void existingImplementationsInheritDefaultMethods() {
        SessionProvider provider = () -> "custom-session";
        OpenTelemetryRum rum =
                new OpenTelemetryRum() {
                    @Override
                    public OpenTelemetry getOpenTelemetry() {
                        return OpenTelemetry.noop();
                    }

                    @Override
                    public SessionProvider getSessionProvider() {
                        return provider;
                    }

                    @Override
                    public Clock getClock() {
                        return Clock.getDefault();
                    }

                    @Override
                    public void emitEvent(String eventName, String body, Attributes attributes) {}

                    @Override
                    public void shutdown() {}
                };

        assertThat(provider.startNewSession()).isFalse();
        assertThat(rum.startNewSession()).isFalse();
        assertThat(provider.getSessionId()).isEqualTo("custom-session");
    }
}
