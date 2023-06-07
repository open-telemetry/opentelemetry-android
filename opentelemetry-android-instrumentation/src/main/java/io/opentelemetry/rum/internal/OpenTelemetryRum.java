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

import android.app.Application;
import io.opentelemetry.api.OpenTelemetry;

/**
 * Entrypoint for the OpenTelemetry Real User Monitoring library for Android.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface OpenTelemetryRum {

    /**
     * Returns a new {@link OpenTelemetryRumBuilder} for {@link OpenTelemetryRum}.
     *
     * @param application The {@link Application} that is being instrumented.
     */
    static OpenTelemetryRumBuilder builder(Application application) {
        return new OpenTelemetryRumBuilder(application);
    }

    /** Returns a no-op implementation of {@link OpenTelemetryRum}. */
    static OpenTelemetryRum noop() {
        return NoopOpenTelemetryRum.INSTANCE;
    }

    /**
     * Get a handle to the instance of the {@linkplain OpenTelemetry OpenTelemetry API} that this
     * instance is using for instrumentation.
     */
    OpenTelemetry getOpenTelemetry();

    /**
     * Get the client session ID associated with this instance of the RUM instrumentation library.
     * Note: this value will change throughout the lifetime of an application instance, so it is
     * recommended that you do not cache this value, but always retrieve it from here when needed.
     */
    String getRumSessionId();
}
