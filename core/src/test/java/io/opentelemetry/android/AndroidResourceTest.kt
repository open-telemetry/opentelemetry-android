/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes
import io.opentelemetry.semconv.TelemetryAttributes
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AndroidResourceTest {
    private val appName: String = "robotron"
    private val rumSdkVersion: String = BuildConfig.OTEL_ANDROID_VERSION
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

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun testFullResource() {
        val appInfo =
            ApplicationInfo().apply {
                labelRes = 12345
            }

        every { ctx.applicationContext.applicationInfo } returns appInfo
        every { ctx.applicationContext.getString(appInfo.labelRes) } returns appName

        val expected =
            Resource
                .getDefault()
                .merge(
                    Resource
                        .builder()
                        .put(ServiceAttributes.SERVICE_NAME, appName)
                        .put(TelemetryAttributes.TELEMETRY_SDK_VERSION, rumSdkVersion)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_NAME, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER)
                        .put(OsIncubatingAttributes.OS_NAME, "Android")
                        .put(OsIncubatingAttributes.OS_TYPE, "linux")
                        .put(OsIncubatingAttributes.OS_VERSION, Build.VERSION.RELEASE)
                        .put(OsIncubatingAttributes.OS_DESCRIPTION, osDescription)
                        .build(),
                )

        val result = AndroidResource.createDefault(ctx)
        assertEquals(expected, result)
    }

    @Test
    fun `fall back to nonLocalizedLabel if needed`() {
        val appInfo =
            ApplicationInfo().apply {
                labelRes = 0
                nonLocalizedLabel = "shim sham"
            }

        every { ctx.applicationContext.applicationInfo } returns appInfo
        every { ctx.applicationInfo } returns appInfo

        val expected =
            Resource
                .getDefault()
                .merge(
                    Resource
                        .builder()
                        .put(ServiceAttributes.SERVICE_NAME, "shim sham")
                        .put(TelemetryAttributes.TELEMETRY_SDK_VERSION, rumSdkVersion)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_NAME, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER)
                        .put(OsIncubatingAttributes.OS_NAME, "Android")
                        .put(OsIncubatingAttributes.OS_TYPE, "linux")
                        .put(OsIncubatingAttributes.OS_VERSION, Build.VERSION.RELEASE)
                        .put(OsIncubatingAttributes.OS_DESCRIPTION, osDescription)
                        .build(),
                )

        val result = AndroidResource.createDefault(ctx)
        assertEquals(expected, result)
    }

    @Test
    fun testProblematicContext() {
        every { ctx.applicationContext.applicationInfo } throws SecurityException("cannot do that")
        every { ctx.applicationContext.resources } throws SecurityException("boom")

        val expected =
            Resource
                .getDefault()
                .merge(
                    Resource
                        .builder()
                        .put(ServiceAttributes.SERVICE_NAME, "unknown_service:android")
                        .put(TelemetryAttributes.TELEMETRY_SDK_VERSION, rumSdkVersion)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_NAME, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER)
                        .put(OsIncubatingAttributes.OS_NAME, "Android")
                        .put(OsIncubatingAttributes.OS_TYPE, "linux")
                        .put(OsIncubatingAttributes.OS_VERSION, Build.VERSION.RELEASE)
                        .put(OsIncubatingAttributes.OS_DESCRIPTION, osDescription)
                        .build(),
                )

        val result = AndroidResource.createDefault(ctx)
        assertEquals(expected, result)
    }
}
