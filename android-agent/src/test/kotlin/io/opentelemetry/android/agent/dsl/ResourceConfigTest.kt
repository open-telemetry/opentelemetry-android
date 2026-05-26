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
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.resources.ResourceBuilder
import io.opentelemetry.kotlin.semconv.ServiceAttributes.SERVICE_NAME
import io.opentelemetry.kotlin.semconv.TelemetryAttributes.TELEMETRY_SDK_LANGUAGE
import io.opentelemetry.kotlin.semconv.TelemetryAttributes.TELEMETRY_SDK_NAME
import io.opentelemetry.kotlin.semconv.TelemetryAttributes.TELEMETRY_SDK_VERSION
import io.opentelemetry.kotlin.semconv.AndroidAttributes.ANDROID_OS_API_LEVEL
import io.opentelemetry.kotlin.semconv.DeviceAttributes.DEVICE_MANUFACTURER
import io.opentelemetry.kotlin.semconv.DeviceAttributes.DEVICE_MODEL_IDENTIFIER
import io.opentelemetry.kotlin.semconv.DeviceAttributes.DEVICE_MODEL_NAME
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_DESCRIPTION
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_NAME
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_TYPE
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_VERSION
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
        assertEquals(customServiceName, attrs[stringKey(SERVICE_NAME)])
        assertEquals("bar", attrs[AttributeKey.stringKey(customKey)])
    }

    private fun assertCommonResources(attrs: Map<AttributeKey<*>, Any>) {
        assertEquals("23", attrs[stringKey(ANDROID_OS_API_LEVEL)])
        assertEquals("unknown", attrs[stringKey(DEVICE_MANUFACTURER)])
        assertEquals("robolectric", attrs[stringKey(DEVICE_MODEL_IDENTIFIER)])
        assertEquals("robolectric", attrs[stringKey(DEVICE_MODEL_NAME)])
        assertEquals("Android", attrs[stringKey(OS_NAME)])
        assertEquals("linux", attrs[stringKey(OS_TYPE)])
        assertEquals("6.0.1", attrs[stringKey(OS_VERSION)])
        assertEquals("Android Version 6.0.1 (Build MMB29M API level 23)", attrs[stringKey(OS_DESCRIPTION)])
        assertEquals("java", attrs[stringKey(TELEMETRY_SDK_LANGUAGE)])
        assertEquals("opentelemetry", attrs[stringKey(TELEMETRY_SDK_NAME)])
        assertNotNull(attrs[stringKey(TELEMETRY_SDK_VERSION)])
    }
}
