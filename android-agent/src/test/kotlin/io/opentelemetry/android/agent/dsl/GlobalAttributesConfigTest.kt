/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.api.common.Attributes
import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalAttributesConfigTest {
    @Test
    fun testDefaults() {
        val otelConfig = OpenTelemetryConfiguration()
        val attrs = otelConfig.rumConfig.getGlobalAttributesSupplier().get()
        assertEquals(Attributes.empty(), attrs)
    }

    @Test
    fun testOverride() {
        val expectedAttrs = Attributes.builder().put("key", "value").build()
        val otelConfig =
            OpenTelemetryConfiguration().apply {
                globalAttributes {
                    expectedAttrs
                }
            }
        val attrs = otelConfig.rumConfig.getGlobalAttributesSupplier().get()
        assertEquals(expectedAttrs, attrs)
    }
}
