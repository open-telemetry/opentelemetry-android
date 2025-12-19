/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import android.os.Build
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME
import io.opentelemetry.semconv.ServiceAttributes.SERVICE_VERSION
import io.opentelemetry.semconv.TelemetryAttributes.TELEMETRY_SDK_VERSION
import io.opentelemetry.semconv.incubating.AndroidIncubatingAttributes.ANDROID_OS_API_LEVEL
import io.opentelemetry.semconv.incubating.AppIncubatingAttributes.APP_INSTALLATION_ID
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MANUFACTURER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_IDENTIFIER
import io.opentelemetry.semconv.incubating.DeviceIncubatingAttributes.DEVICE_MODEL_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_DESCRIPTION
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_NAME
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_TYPE
import io.opentelemetry.semconv.incubating.OsIncubatingAttributes.OS_VERSION
import java.util.UUID

private const val SHARED_PREF_FILE = "opentelemetry-android"
private const val DEFAULT_APP_NAME = "unknown_service:android"

object AndroidResource {
    @JvmStatic
    fun createDefault(context: Context): Resource {
        val appName = readAppName(context)
        val resourceBuilder =
            Resource.getDefault().toBuilder().put(SERVICE_NAME, appName)
        val appVersion = readAppVersion(context)
        appVersion?.let { resourceBuilder.put(SERVICE_VERSION, it) }

        return resourceBuilder
            .put(TELEMETRY_SDK_VERSION, BuildConfig.OTEL_ANDROID_VERSION)
            .put(DEVICE_MODEL_NAME, Build.MODEL)
            .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
            .put(DEVICE_MANUFACTURER, Build.MANUFACTURER)
            .put(OS_NAME, "Android")
            .put(ANDROID_OS_API_LEVEL, Build.VERSION.SDK_INT.toString())
            .put(OS_TYPE, "linux")
            .put(OS_VERSION, Build.VERSION.RELEASE)
            .put(OS_DESCRIPTION, oSDescription)
            .put(APP_INSTALLATION_ID, readInstallId(context))
            .build()
    }

    private fun readInstallId(context: Context): String {
        // install ID is persisted using the app.installation.id semconv as its key
        val prefs = context.getSharedPreferences(SHARED_PREF_FILE, 0)
        val installId = prefs.getString(APP_INSTALLATION_ID.key, null)

        if (installId == null) {
            val id = UUID.randomUUID().toString()
            prefs.edit().putString(APP_INSTALLATION_ID.key, id).apply()
            return id
        }
        return installId
    }

    private fun readAppName(context: Context): String =
        try {
            val ctx = context.applicationContext
            val stringId =
                ctx.applicationInfo.labelRes
            if (stringId == 0) {
                ctx.applicationInfo.nonLocalizedLabel.toString()
            } else {
                ctx.getString(stringId)
            }
        } catch (_: Exception) {
            DEFAULT_APP_NAME
        }

    private fun readAppVersion(context: Context): String? =
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (_: Exception) {
            null
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
