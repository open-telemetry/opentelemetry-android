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

import io.opentelemetry.rum.internal.instrumentation.ApplicationStateListener;
import io.opentelemetry.rum.internal.instrumentation.InstrumentedApplication;
import io.opentelemetry.sdk.OpenTelemetrySdk;

final class InstrumentedApplicationImpl implements InstrumentedApplication {

    private final Application application;
    private final OpenTelemetrySdk openTelemetrySdk;
    private final ApplicationStateWatcher applicationStateWatcher;

    InstrumentedApplicationImpl(
            Application application,
            OpenTelemetrySdk openTelemetrySdk,
            ApplicationStateWatcher applicationStateWatcher) {
        this.application = application;
        this.openTelemetrySdk = openTelemetrySdk;
        this.applicationStateWatcher = applicationStateWatcher;
    }

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public OpenTelemetrySdk getOpenTelemetrySdk() {
        return openTelemetrySdk;
    }

    @Override
    public void registerApplicationStateListener(ApplicationStateListener listener) {
        applicationStateWatcher.registerListener(listener);
    }
}
