/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.extensions.ApiExtension
import io.opentelemetry.android.extensions.extension
import io.opentelemetry.api.OpenTelemetry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OpenTelemetryRumNoopTest {
    @Test
    fun testNoopOpenTelemetryRum() {
        val noop = RumBuilder.noop()
        assertEquals(OpenTelemetry.noop(), noop.openTelemetry)
        assertEquals("", noop.sessionProvider.getSessionId())
        assertThat(noop.getExtension(ApiExtension::class.java)).isNull()
        assertThat(noop.extension<ApiExtension>()).isNull()

        // assert no exceptions thrown
        noop.emitEvent("event")
        noop.shutdown()
    }
}
