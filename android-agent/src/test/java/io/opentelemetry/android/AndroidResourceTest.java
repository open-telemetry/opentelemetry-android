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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AndroidResourceTest {

    String appName = "robotron";
    String rumSdkVersion = BuildConfig.OTEL_ANDROID_VERSION;
    String osDescription =
            new StringBuilder()
                    .append("Android Version ")
                    .append(Build.VERSION.RELEASE)
                    .append(" (Build ")
                    .append(Build.ID)
                    .append(" API level ")
                    .append(Build.VERSION.SDK_INT)
                    .append(")")
                    .toString();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Application app;

    @Test
    void testFullResource() {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.labelRes = 12345;

        when(app.getApplicationContext().getApplicationInfo()).thenReturn(appInfo);
        when(app.getApplicationContext().getString(appInfo.labelRes)).thenReturn(appName);

        Resource expected =
                Resource.getDefault()
                        .merge(
                                Resource.builder()
                                        .put(SERVICE_NAME, appName)
                                        .put(RUM_SDK_VERSION, rumSdkVersion)
                                        .put(DEVICE_MODEL_NAME, Build.MODEL)
                                        .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                                        .put(DEVICE_MANUFACTURER, Build.MANUFACTURER)
                                        .put(OS_NAME, "Android")
                                        .put(OS_TYPE, "linux")
                                        .put(OS_VERSION, Build.VERSION.RELEASE)
                                        .put(OS_DESCRIPTION, osDescription)
                                        .build());

        Resource result = AndroidResource.createDefault(app);
        assertEquals(expected, result);
    }

    @Test
    void testProblematicContext() {
        when(app.getApplicationContext().getApplicationInfo())
                .thenThrow(new SecurityException("cannot do that"));
        when(app.getApplicationContext().getResources()).thenThrow(new SecurityException("boom"));

        Resource expected =
                Resource.getDefault()
                        .merge(
                                Resource.builder()
                                        .put(SERVICE_NAME, "unknown_service:android")
                                        .put(RUM_SDK_VERSION, rumSdkVersion)
                                        .put(DEVICE_MODEL_NAME, Build.MODEL)
                                        .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                                        .put(DEVICE_MANUFACTURER, Build.MANUFACTURER)
                                        .put(OS_NAME, "Android")
                                        .put(OS_TYPE, "linux")
                                        .put(OS_VERSION, Build.VERSION.RELEASE)
                                        .put(OS_DESCRIPTION, osDescription)
                                        .build());

        Resource result = AndroidResource.createDefault(app);
        assertEquals(expected, result);
    }
}
