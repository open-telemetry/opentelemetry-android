/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
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

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private lateinit var app: Application

    @Test
    fun testFullResource() {
        val appInfo = ApplicationInfo()
        appInfo.labelRes = 12345

        Mockito
            .`when`(app.applicationContext.applicationInfo)
            .thenReturn(appInfo)
        Mockito
            .`when`(app.applicationContext.getString(appInfo.labelRes))
            .thenReturn(appName)

        val expected =
            Resource
                .getDefault()
                .merge(
                    Resource
                        .builder()
                        .put(ServiceAttributes.SERVICE_NAME, appName)
                        .put(RumConstants.RUM_SDK_VERSION, rumSdkVersion)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_NAME, Build.MODEL)
                        .put(
                            DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER,
                            Build.MODEL,
                        ).put(
                            DeviceIncubatingAttributes.DEVICE_MANUFACTURER,
                            Build.MANUFACTURER,
                        ).put(OsIncubatingAttributes.OS_NAME, "Android")
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
        Mockito
            .`when`(app.applicationContext.applicationInfo)
            .thenThrow(SecurityException("cannot do that"))
        Mockito.`when`(app.applicationContext.resources).thenThrow(
            SecurityException("boom"),
        )

        val expected =
            Resource
                .getDefault()
                .merge(
                    Resource
                        .builder()
                        .put(ServiceAttributes.SERVICE_NAME, "unknown_service:android")
                        .put(RumConstants.RUM_SDK_VERSION, rumSdkVersion)
                        .put(DeviceIncubatingAttributes.DEVICE_MODEL_NAME, Build.MODEL)
                        .put(
                            DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER,
                            Build.MODEL,
                        ).put(
                            DeviceIncubatingAttributes.DEVICE_MANUFACTURER,
                            Build.MANUFACTURER,
                        ).put(OsIncubatingAttributes.OS_NAME, "Android")
                        .put(OsIncubatingAttributes.OS_TYPE, "linux")
                        .put(OsIncubatingAttributes.OS_VERSION, Build.VERSION.RELEASE)
                        .put(OsIncubatingAttributes.OS_DESCRIPTION, osDescription)
                        .build(),
                )

        val result = AndroidResource.createDefault(app)
        assertEquals(expected, result)
    }
}
