/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.Attributes
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test

class GlobalAttributesConfigTest {
    @Test
    fun testDefaults() {
        val otelConfig = OpenTelemetryConfiguration(clock = FakeClock())
        val attrs = otelConfig.rumConfig.getGlobalAttributesSupplier().get()
        assertEquals(Attributes.empty(), attrs)
    }

    @Test
    fun testOverride() {
        val expectedAttrs = Attributes.builder().put("key", "value").build()
        val otelConfig =
            OpenTelemetryConfiguration(clock = FakeClock()).apply {
                globalAttributes {
                    expectedAttrs
                }
            }
        val attrs = otelConfig.rumConfig.getGlobalAttributesSupplier().get()
        assertEquals(expectedAttrs, attrs)
    }

    @Test
    fun testOverrideSupplier() {
        val otelConfig =
            OpenTelemetryConfiguration(clock = FakeClock()).apply {
                globalAttributesSupplier {
                    {
                        Attributes.builder().put("key", System.nanoTime()).build()
                    }
                }
            }
        val supplier = otelConfig.rumConfig.getGlobalAttributesSupplier()
        val attrValue1 = supplier.get().get(longKey("key"))
        val attrValue2 = supplier.get().get(longKey("key"))

        assertThat(attrValue1).isNotNull()
        assertThat(attrValue2).isNotNull()
        assertThat(attrValue2).isGreaterThan(attrValue1)
    }
}
