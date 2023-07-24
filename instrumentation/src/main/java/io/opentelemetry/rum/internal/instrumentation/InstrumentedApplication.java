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

package io.opentelemetry.rum.internal.instrumentation;

import android.app.Application;
import io.opentelemetry.rum.internal.OpenTelemetryRum;
import io.opentelemetry.sdk.OpenTelemetrySdk;

/**
 * Provides access to the {@linkplain OpenTelemetrySdk OpenTelemetry SDK}, the instrumented {@link
 * Application}, allows registering {@linkplain ApplicationStateListener listeners}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface InstrumentedApplication {

    /** Returns the instrumented {@link Application}. */
    Application getApplication();

    /**
     * Returns the {@link OpenTelemetrySdk} that is a part of the constructed {@link
     * OpenTelemetryRum}.
     */
    OpenTelemetrySdk getOpenTelemetrySdk();

    /**
     * Registers the passed {@link ApplicationStateListener} - from now on it will be called
     * whenever the application is moved from background to foreground, and vice versa.
     *
     * <p>Users of this method should take care to avoid passing the same listener instance multiple
     * times; duplicates are not trimmed.
     */
    void registerApplicationStateListener(ApplicationStateListener listener);
}
