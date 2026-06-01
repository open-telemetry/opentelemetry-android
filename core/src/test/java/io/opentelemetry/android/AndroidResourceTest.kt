/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Build
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.resources.ResourceBuilder
import io.opentelemetry.kotlin.semconv.TelemetryAttributes
import io.opentelemetry.kotlin.semconv.AndroidAttributes
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_INSTALLATION_ID
import io.opentelemetry.kotlin.semconv.DeviceAttributes
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.OsAttributes
import io.opentelemetry.kotlin.semconv.ServiceAttributes.SERVICE_NAME
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.util.UUID

internal class AndroidResourceTest {
    private val appName: String = "robotron"
    private val prefsName: String = "opentelemetry-android"
    private val rumSdkVersion: String = BuildConfig.OTEL_ANDROID_VERSION
    private val installId: String = "install-id"
    private val osDescription: String =
        "Android Version " +
            Build.VERSION.RELEASE +
            " (Build " +
            Build.ID +
            " API level " +
            Build.VERSION.SDK_INT +
            ")"

    @RelaxedMockK
    private lateinit var ctx: Context
    private lateinit var expectedResourceBuilder: ResourceBuilder
    private lateinit var appInfo: ApplicationInfo

    @OptIn(IncubatingApi::class)
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        every { ctx.getSharedPreferences(prefsName, 0) } returns
            mockk {
                every {
                    getString(
                        APP_INSTALLATION_ID,
                        null,
                    )
                } returns installId
            }

        appInfo =
            ApplicationInfo().apply {
                labelRes = 12345
            }

        every { ctx.applicationContext.applicationInfo } returns appInfo
        every { ctx.applicationContext.getString(appInfo.labelRes) } returns appName

        expectedResourceBuilder =
            Resource
                .builder()
                .put(SERVICE_NAME, appName)
                .put(TelemetryAttributes.TELEMETRY_SDK_VERSION, rumSdkVersion)
                .put(DeviceAttributes.DEVICE_MODEL_NAME, Build.MODEL)
                .put(DeviceAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                .put(DeviceAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER)
                .put(OsAttributes.OS_NAME, "Android")
                .put(OsAttributes.OS_TYPE, "linux")
                .put(OsAttributes.OS_VERSION, Build.VERSION.RELEASE)
                .put(
                    AndroidAttributes.ANDROID_OS_API_LEVEL,
                    Build.VERSION.SDK_INT.toString(),
                ).put(OsAttributes.OS_DESCRIPTION, osDescription)
                .put(APP_INSTALLATION_ID, installId)
    }

    @Test
    fun testFullResource() {
        assertResourceMatches()
    }

    @Test
    fun `fall back to nonLocalizedLabel if needed`() {
        appInfo =
            ApplicationInfo().apply {
                labelRes = 0
                nonLocalizedLabel = "shim sham"
            }
        every { ctx.applicationContext.applicationInfo } returns appInfo

        assertResourceMatches(
            extraAttributes = mapOf(stringKey(SERVICE_NAME) to "shim sham"),
        )
    }

    @Test
    fun testProblematicContext() {
        every { ctx.applicationContext.applicationInfo } throws SecurityException("cannot do that")
        every { ctx.applicationContext.resources } throws SecurityException("boom")

        assertResourceMatches(
            extraAttributes = mapOf(stringKey(SERVICE_NAME) to "unknown_service:android"),
        )
    }

    @OptIn(IncubatingApi::class)
    @Test
    fun `test install id generated if none available`() {
        val slot = slot<String>()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)

        every { ctx.getSharedPreferences(prefsName, 0) } returns
            mockk {
                every {
                    getString(
                        APP_INSTALLATION_ID,
                        null,
                    )
                } returns null
                every { edit() } returns editor
            }

        every {
            editor.putString(
                APP_INSTALLATION_ID,
                capture(slot),
            )
        } returns editor

        assertResourceMatches(
            resource = AndroidResource.createDefault(ctx),
            extraAttributes = mapOf(stringKey(APP_INSTALLATION_ID) to slot.captured),
        )
        assertNotNull(UUID.fromString(slot.captured))
    }

    private fun assertResourceMatches(
        resource: Resource = AndroidResource.createDefault(ctx),
        extraAttributes: Map<AttributeKey<*>, String> = emptyMap(),
    ) {
        extraAttributes.forEach { entry ->
            expectedResourceBuilder.put(entry.key.key, entry.value)
        }
        val expected = Resource.getDefault().merge(expectedResourceBuilder.build())
        assertEquals(expected, resource)
    }
}
