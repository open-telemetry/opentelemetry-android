/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes
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
    private lateinit var app: Application

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

        every { app.applicationInfo } returns appInfo
        every { app.applicationContext.getString(appInfo.labelRes) } returns appName

        val expected =
            Resource
                .getDefault()
                .merge(
                    Resource
                        .builder()
                        .put(ServiceAttributes.SERVICE_NAME, appName)
                        .put(RumConstants.RUM_SDK_VERSION, rumSdkVersion)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_NAME, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER)
                        .put(OsIncubatingAttributes.OS_NAME, "Android")
                        .put(OsIncubatingAttributes.OS_TYPE, "linux")
                        .put(OsIncubatingAttributes.OS_VERSION, Build.VERSION.RELEASE)
                        .put(OsIncubatingAttributes.OS_DESCRIPTION, osDescription)
                        .build(),
                )

        val result = AndroidResource.createDefault(app)
        assertEquals(expected, result)
    }

    @Test
    fun `fall back to nonLocalizedLabel if needed`() {
        val appInfo =
            ApplicationInfo().apply {
                labelRes = 0
                nonLocalizedLabel = "shim sham"
            }

        every { app.applicationContext.applicationInfo } returns appInfo
        every { app.applicationInfo } returns appInfo

        val expected =
            Resource
                .getDefault()
                .merge(
                    Resource
                        .builder()
                        .put(ServiceAttributes.SERVICE_NAME, "shim sham")
                        .put(RumConstants.RUM_SDK_VERSION, rumSdkVersion)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_NAME, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER)
                        .put(OsIncubatingAttributes.OS_NAME, "Android")
                        .put(OsIncubatingAttributes.OS_TYPE, "linux")
                        .put(OsIncubatingAttributes.OS_VERSION, Build.VERSION.RELEASE)
                        .put(OsIncubatingAttributes.OS_DESCRIPTION, osDescription)
                        .build(),
                )

        val result = AndroidResource.createDefault(app)
        assertEquals(expected, result)
    }

    @Test
    fun testProblematicContext() {
        every { app.applicationContext.applicationInfo } throws SecurityException("cannot do that")
        every { app.applicationContext.resources } throws SecurityException("boom")

        val expected =
            Resource
                .getDefault()
                .merge(
                    Resource
                        .builder()
                        .put(ServiceAttributes.SERVICE_NAME, "unknown_service:android")
                        .put(RumConstants.RUM_SDK_VERSION, rumSdkVersion)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_NAME, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                        .put(DeviceIncubatingAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER)
                        .put(OsIncubatingAttributes.OS_NAME, "Android")
                        .put(OsIncubatingAttributes.OS_TYPE, "linux")
                        .put(OsIncubatingAttributes.OS_VERSION, Build.VERSION.RELEASE)
                        .put(OsIncubatingAttributes.OS_DESCRIPTION, osDescription)
                        .build(),
                )

        val result = AndroidResource.createDefault(app)
        assertEquals(expected, result)
    }
}
