/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.api.OpenTelemetry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OpenTelemetryRumNoopTest {
    @Test
    fun testNoopOpenTelemetryRum() {
        val noop = RumBuilder.noop()
        assertEquals(OpenTelemetry.noop(), noop.openTelemetry)
        assertEquals("00000000000000000000000000000000", noop.getRumSessionId())

        // assert no exceptions thrown
        noop.emitEvent("event")
        noop.shutdown()
    }
}
