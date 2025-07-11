/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import android.os.Build
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes

object AndroidResource {
    @JvmStatic
    fun createDefault(application: Application): Resource {
        val appName = readAppName(application)
        val resourceBuilder =
            Resource.getDefault().toBuilder().put(ServiceAttributes.SERVICE_NAME, appName)

        return resourceBuilder
            .put(RumConstants.RUM_SDK_VERSION, BuildConfig.OTEL_ANDROID_VERSION)
            .put(DeviceIncubatingAttributes.DEVICE_MODEL_NAME, Build.MODEL)
            .put(DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER, Build.MODEL)
            .put(DeviceIncubatingAttributes.DEVICE_MANUFACTURER, Build.MANUFACTURER)
            .put(OsIncubatingAttributes.OS_NAME, "Android")
            .put(OsIncubatingAttributes.OS_TYPE, "linux")
            .put(OsIncubatingAttributes.OS_VERSION, Build.VERSION.RELEASE)
            .put(OsIncubatingAttributes.OS_DESCRIPTION, oSDescription)
            .build()
    }

    private fun readAppName(application: Application): String =
        try {
            val stringId =
                application.applicationContext.applicationInfo.labelRes
            application.applicationContext.getString(stringId)
        } catch (_: Exception) {
            "unknown_service:android"
        }

    private val oSDescription: String
        get() {
            val osDescriptionBuilder = StringBuilder()
            return osDescriptionBuilder
                .append("Android Version ")
                .append(Build.VERSION.RELEASE)
                .append(" (Build ")
                .append(Build.ID)
                .append(" API level ")
                .append(Build.VERSION.SDK_INT)
                .append(")")
                .toString()
        }
}
