/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.kotlin.semconv.AndroidAttributes.ANDROID_OS_API_LEVEL
import io.opentelemetry.kotlin.semconv.DeviceAttributes.DEVICE_MANUFACTURER
import io.opentelemetry.kotlin.semconv.DeviceAttributes.DEVICE_MODEL_IDENTIFIER
import io.opentelemetry.kotlin.semconv.DeviceAttributes.DEVICE_MODEL_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_BUILD_ID
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_DESCRIPTION
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_NAME
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_TYPE
import io.opentelemetry.kotlin.semconv.OsAttributes.OS_VERSION
import io.opentelemetry.kotlin.semconv.ServiceAttributes.SERVICE_NAME
import io.opentelemetry.kotlin.semconv.TelemetryAttributes.TELEMETRY_SDK_LANGUAGE
import io.opentelemetry.kotlin.semconv.TelemetryAttributes.TELEMETRY_SDK_NAME
import io.opentelemetry.kotlin.semconv.TelemetryAttributes.TELEMETRY_SDK_VERSION
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.resources.ResourceBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(IncubatingApi::class)
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
        assertEquals("bar", attrs[stringKey(customKey)])
    }

    @Test
    fun testResourceReplacement() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val customResource =
            Resource
                .builder()
                .put("custom.key", "custom.value")
                .build()
        lateinit var builder: ResourceBuilder

        OpenTelemetryRumInitializer.initialize(ctx) {
            resource(customResource)
            resource {
                builder = this
            }
        }

        val resource = builder.build()
        val attrs = resource.attributes.asMap()
        assertEquals("custom.value", attrs[stringKey("custom.key")])
        assertNull(attrs[stringKey(ANDROID_OS_API_LEVEL)])
        assertNull(attrs[stringKey(DEVICE_MANUFACTURER)])
        assertNull(attrs[stringKey(DEVICE_MODEL_NAME)])
        assertNull(attrs[stringKey(OS_NAME)])
    }

    @Test
    fun testResourceReplacementWithAction() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val customResource =
            Resource
                .builder()
                .put("custom.key", "custom.value")
                .build()
        lateinit var builder: ResourceBuilder

        OpenTelemetryRumInitializer.initialize(ctx) {
            resource(customResource) {
                builder = this
                put("extra.key", "extra.value")
            }
        }

        val resource = builder.build()
        val attrs = resource.attributes.asMap()
        assertEquals("custom.value", attrs[stringKey("custom.key")])
        assertEquals("extra.value", attrs[stringKey("extra.key")])
        assertNull(attrs[stringKey(ANDROID_OS_API_LEVEL)])
        assertNull(attrs[stringKey(DEVICE_MANUFACTURER)])
    }

    @Test
    fun testSetResource() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val customResource =
            Resource
                .builder()
                .put("service.name", "my-service")
                .build()
        val cfg = OpenTelemetryConfiguration(instrumentationLoader = mockk(relaxed = true))

        cfg.setResource(customResource)
        val resource = cfg.resourceProvider(ctx)

        assertEquals(customResource, resource)
        assertNull(resource.attributes.get(stringKey(ANDROID_OS_API_LEVEL)))
    }

    @Test
    fun testResourceDirect() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val customResource =
            Resource
                .builder()
                .put("service.name", "my-service")
                .build()
        val cfg = OpenTelemetryConfiguration(instrumentationLoader = mockk(relaxed = true))

        cfg.resource(customResource)
        val resource = cfg.resourceProvider(ctx)

        assertEquals(customResource, resource)
        assertNull(resource.attributes.get(stringKey(ANDROID_OS_API_LEVEL)))
    }

    private fun assertCommonResources(attrs: Map<AttributeKey<*>, Any>) {
        assertEquals("23", attrs[stringKey(ANDROID_OS_API_LEVEL)])
        assertEquals("unknown", attrs[stringKey(DEVICE_MANUFACTURER)])
        assertEquals("robolectric", attrs[stringKey(DEVICE_MODEL_IDENTIFIER)])
        assertEquals("robolectric", attrs[stringKey(DEVICE_MODEL_NAME)])
        assertEquals("Android", attrs[stringKey(OS_NAME)])
        assertEquals("linux", attrs[stringKey(OS_TYPE)])
        assertEquals("6.0.1", attrs[stringKey(OS_VERSION)])
        assertEquals("MMB29M", attrs[stringKey(OS_BUILD_ID)])
        assertEquals("Android Version 6.0.1 (Build MMB29M API level 23)", attrs[stringKey(OS_DESCRIPTION)])
        assertEquals("java", attrs[stringKey(TELEMETRY_SDK_LANGUAGE)])
        assertEquals("opentelemetry", attrs[stringKey(TELEMETRY_SDK_NAME)])
        assertNotNull(attrs[stringKey(TELEMETRY_SDK_VERSION)])
    }
}
