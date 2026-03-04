/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.Attributes
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GlobalAttributesConfigTest {

    private lateinit var otelConfig: OpenTelemetryConfiguration

    @Before
    fun setUp() {
        otelConfig = OpenTelemetryConfiguration(
            clock = FakeClock(),
            instrumentationLoader = FakeInstrumentationLoader()
        )
    }

    @Test
    fun testDefaults() {
        val attrs = otelConfig.rumConfig.getGlobalAttributesSupplier().get()
        assertEquals(Attributes.empty(), attrs)
    }

    @Test
    fun testOverride() {
        val expectedAttrs = Attributes.builder().put("key", "value").build()
        val otelConfig = otelConfig.apply {
            globalAttributes {
                expectedAttrs
            }
        }
        val attrs = otelConfig.rumConfig.getGlobalAttributesSupplier().get()
        assertEquals(expectedAttrs, attrs)
    }

    @Test
    fun testOverrideSupplier() {
        val otelConfig = otelConfig.apply {
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
