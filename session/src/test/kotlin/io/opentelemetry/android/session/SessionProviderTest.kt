/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

import io.opentelemetry.android.session.SessionProvider.Companion.NO_SESSION_ID
import io.opentelemetry.android.session.SessionProvider.Companion.getNoop
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SessionProviderTest {
    @Test
    fun `noop provider returns empty session identifiers`() {
        // Given / When
        val noop = getNoop()

        // Then
        assertThat(noop.getSessionId()).isEqualTo(NO_SESSION_ID).isEmpty()
        assertThat(noop.getPreviousSessionId()).isEqualTo(NO_SESSION_ID).isEmpty()
    }
}
