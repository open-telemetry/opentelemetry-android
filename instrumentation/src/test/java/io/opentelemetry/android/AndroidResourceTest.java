/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.RumConstants.RUM_SDK_VERSION;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEVICE_MODEL_IDENTIFIER;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEVICE_MODEL_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_TYPE;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_VERSION;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;
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
    String rumSdkVersion = "1.2.3";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Application app;

    @Test
    void testFullResource() {
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.labelRes = 12345;
        when(app.getApplicationContext().getApplicationInfo()).thenReturn(appInfo);
        when(app.getApplicationContext().getString(appInfo.labelRes)).thenReturn(appName);
        when(app.getApplicationContext().getResources().getString(R.string.rum_version))
                .thenReturn(rumSdkVersion);

        Resource expected =
                Resource.getDefault()
                        .merge(
                                Resource.builder()
                                        .put(SERVICE_NAME, appName)
                                        .put(RUM_SDK_VERSION, rumSdkVersion)
                                        .put(DEVICE_MODEL_NAME, Build.MODEL)
                                        .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                                        .put(OS_NAME, "Android")
                                        .put(OS_TYPE, "linux")
                                        .put(OS_VERSION, Build.VERSION.RELEASE)
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
                                        .put(RUM_SDK_VERSION, "unknown")
                                        .put(DEVICE_MODEL_NAME, Build.MODEL)
                                        .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                                        .put(OS_NAME, "Android")
                                        .put(OS_TYPE, "linux")
                                        .put(OS_VERSION, Build.VERSION.RELEASE)
                                        .build());

        Resource result = AndroidResource.createDefault(app);
        assertEquals(expected, result);
    }
}
