/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.internal.tools.RumConstants.RUM_SDK_VERSION;
import static io.opentelemetry.semconv.ResourceAttributes.DEVICE_MANUFACTURER;
import static io.opentelemetry.semconv.ResourceAttributes.DEVICE_MODEL_IDENTIFIER;
import static io.opentelemetry.semconv.ResourceAttributes.DEVICE_MODEL_NAME;
import static io.opentelemetry.semconv.ResourceAttributes.OS_DESCRIPTION;
import static io.opentelemetry.semconv.ResourceAttributes.OS_NAME;
import static io.opentelemetry.semconv.ResourceAttributes.OS_TYPE;
import static io.opentelemetry.semconv.ResourceAttributes.OS_VERSION;
import static io.opentelemetry.semconv.ResourceAttributes.SERVICE_NAME;

import android.app.Application;
import android.os.Build;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import java.util.function.Supplier;

final class AndroidResource {

    static Resource createDefault(Application application) {
        String appName = readAppName(application);
        ResourceBuilder resourceBuilder =
                Resource.getDefault().toBuilder().put(SERVICE_NAME, appName);

        return resourceBuilder
                .put(RUM_SDK_VERSION, BuildConfig.OTEL_ANDROID_VERSION)
                .put(DEVICE_MODEL_NAME, Build.MODEL)
                .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                .put(DEVICE_MANUFACTURER, Build.MANUFACTURER)
                .put(OS_NAME, "Android")
                .put(OS_TYPE, "linux")
                .put(OS_VERSION, Build.VERSION.RELEASE)
                .put(OS_DESCRIPTION, getOSDescription())
                .build();
    }

    private static String readAppName(Application application) {
        return trapTo(
                () -> {
                    int stringId =
                            application.getApplicationContext().getApplicationInfo().labelRes;
                    return application.getApplicationContext().getString(stringId);
                },
                "unknown_service:android");
    }

    private static String trapTo(Supplier<String> fn, String defaultValue) {
        try {
            return fn.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String getOSDescription() {
        StringBuilder osDescriptionBuilder = new StringBuilder();
        return osDescriptionBuilder
                .append("Android Version ")
                .append(Build.VERSION.RELEASE)
                .append(" (Build ")
                .append(Build.ID)
                .append(" API level ")
                .append(Build.VERSION.SDK_INT)
                .append(")")
                .toString();
    }
}
