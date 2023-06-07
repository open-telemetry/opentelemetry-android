/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal;

import static io.opentelemetry.rum.internal.RumConstants.RUM_SDK_VERSION;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEVICE_MODEL_IDENTIFIER;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEVICE_MODEL_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_TYPE;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_VERSION;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;

import android.app.Application;
import android.os.Build;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import java.util.function.Supplier;
import opentelemetry.rum.instrumentation.R;

final class AndroidResource {

    static Resource createDefault(Application application) {
        String appName = readAppName(application);
        ResourceBuilder resourceBuilder =
                Resource.getDefault().toBuilder().put(SERVICE_NAME, appName);

        return resourceBuilder
                .put(RUM_SDK_VERSION, detectRumVersion(application))
                .put(DEVICE_MODEL_NAME, Build.MODEL)
                .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                .put(OS_NAME, "Android")
                .put(OS_TYPE, "linux")
                .put(OS_VERSION, Build.VERSION.RELEASE)
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

    private static String detectRumVersion(Application application) {
        return trapTo(
                () -> {
                    // TODO: Verify that this will be in the lib/jar at runtime.
                    // TODO: After donation, package of R file will change
                    return application
                            .getApplicationContext()
                            .getResources()
                            .getString(R.string.rum_version);
                },
                "unknown");
    }

    private static String trapTo(Supplier<String> fn, String defaultValue) {
        try {
            return fn.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
