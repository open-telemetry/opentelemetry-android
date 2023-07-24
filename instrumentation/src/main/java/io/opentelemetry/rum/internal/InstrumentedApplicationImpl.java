/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
