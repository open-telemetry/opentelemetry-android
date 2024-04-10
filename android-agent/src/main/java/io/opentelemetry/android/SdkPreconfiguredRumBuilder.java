/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import android.app.Application;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.internal.services.ServiceManager;
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleService;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.ArrayList;
import java.util.List;

public final class SdkPreconfiguredRumBuilder {
    private final Application application;
    private final OpenTelemetrySdk sdk;
    private final SessionId sessionId;
    private final boolean discoverInstrumentations;

    private final List<AndroidInstrumentation> instrumentations = new ArrayList<>();

    SdkPreconfiguredRumBuilder(
            Application application,
            OpenTelemetrySdk openTelemetrySdk,
            boolean discoverInstrumentations) {
        this(
                application,
                openTelemetrySdk,
                new SessionId(new SessionIdTimeoutHandler()),
                discoverInstrumentations);
    }

    SdkPreconfiguredRumBuilder(
            Application application,
            OpenTelemetrySdk openTelemetrySdk,
            SessionId sessionId,
            boolean discoverInstrumentations) {
        this.application = application;
        this.sdk = openTelemetrySdk;
        this.sessionId = sessionId;
        this.discoverInstrumentations = discoverInstrumentations;
    }

    /**
     * Adds an instrumentation to be applied as a part of the {@link #build()} method call.
     *
     * @return {@code this}
     */
    public SdkPreconfiguredRumBuilder addInstrumentation(AndroidInstrumentation instrumentation) {
        instrumentations.add(instrumentation);
        return this;
    }

    /**
     * Creates a new instance of {@link OpenTelemetryRum} with the settings of this {@link
     * OpenTelemetryRumBuilder}.
     *
     * <p>This method uses a preconfigured OpenTelemetry SDK and install built-in system
     * instrumentations in the passed Android {@link Application}.
     *
     * @return A new {@link OpenTelemetryRum} instance.
     */
    public OpenTelemetryRum build() {
        ServiceManager.get()
                .getService(AppLifecycleService.class)
                .registerListener(sessionId.getTimeoutHandler());

        Tracer tracer = sdk.getTracer(OpenTelemetryRum.class.getSimpleName());
        sessionId.setSessionIdChangeListener(new SessionIdChangeTracer(tracer));

        OpenTelemetryRumImpl openTelemetryRum = new OpenTelemetryRumImpl(sdk, sessionId);

        // Apply instrumentations
        for (AndroidInstrumentation instrumentation : getInstrumentations()) {
            instrumentation.apply(application, openTelemetryRum);
        }

        return openTelemetryRum;
    }

    private List<AndroidInstrumentation> getInstrumentations() {
        if (discoverInstrumentations) {
            instrumentations.addAll(AndroidInstrumentation.getAll());
        }

        return instrumentations;
    }
}
