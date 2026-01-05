/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.resources.ResourceBuilder
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME
import io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_LANGUAGE
import io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_NAME
import io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_VERSION
import io.opentelemetry.semconv.incubating.AndroidIncubatingAttributes.ANDROID_OS_API_LEVEL
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MANUFACTURER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_DESCRIPTION
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResourceConfigTest {
    @Test
    fun testDefaults() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        lateinit var builder: ResourceBuilder

        OpenTelemetryRumInitializer.initialize(ctx) {
            resource {
                builder = this
            }
        }

        val resource = builder.build()
        val attrs = resource.attributes.asMap()
        assertCommonResources(attrs)
    }

    @Test
    fun testOverrides() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        lateinit var builder: ResourceBuilder
        val customKey = "foo"
        val customValue = "bar"
        val customServiceName = "test-service"

        OpenTelemetryRumInitializer.initialize(ctx) {
            resource {
                builder = this
                put(customKey, customValue)
                put(SERVICE_NAME, customServiceName)
            }
        }

        val resource = builder.build()
        val attrs = resource.attributes.asMap()
        assertCommonResources(attrs)
        assertEquals(customServiceName, attrs[SERVICE_NAME])
        assertEquals("bar", attrs[AttributeKey.stringKey(customKey)])
    }

    private fun assertCommonResources(attrs: Map<AttributeKey<*>, Any>) {
        assertEquals("23", attrs[ANDROID_OS_API_LEVEL])
        assertEquals("unknown", attrs[DEVICE_MANUFACTURER])
        assertEquals("robolectric", attrs[DEVICE_MODEL_IDENTIFIER])
        assertEquals("robolectric", attrs[DEVICE_MODEL_NAME])
        assertEquals("Android", attrs[OS_NAME])
        assertEquals("linux", attrs[OS_TYPE])
        assertEquals("6.0.1", attrs[OS_VERSION])
        assertEquals("Android Version 6.0.1 (Build MMB29M API level 23)", attrs[OS_DESCRIPTION])
        assertEquals("java", attrs[TELEMETRY_SDK_LANGUAGE])
        assertEquals("opentelemetry", attrs[TELEMETRY_SDK_NAME])
        assertNotNull(attrs[TELEMETRY_SDK_VERSION])
    }
}
