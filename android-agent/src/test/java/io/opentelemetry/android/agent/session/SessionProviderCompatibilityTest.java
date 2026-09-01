/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.android.session.SessionProvider;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SessionProviderCompatibilityTest {
    @Test
    void existingJavaProvidersUseDefaultActivityMethods() {
        AtomicInteger calls = new AtomicInteger();
        SessionProvider provider =
                () -> {
                    calls.incrementAndGet();
                    return "session-id";
                };

        assertThat(provider.getSessionIdForAttribution()).isEqualTo("session-id");
        provider.recordActivity();

        assertThat(calls).hasValue(2);
    }
}
