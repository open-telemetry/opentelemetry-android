/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import android.app.Application;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.internal.services.Services;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

/**
 * Entrypoint for the OpenTelemetry Real User Monitoring library for Android.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public interface OpenTelemetryRum {

    /**
     * Returns a new {@link OpenTelemetryRumBuilder} for {@link OpenTelemetryRum} with a default
     * configuration. Use this version if you would like to configure individual aspects of the
     * OpenTelemetry SDK but would still prefer to allow OpenTelemetry RUM to create the SDK for
     * you. For additional configuration, call the two-argument version of build and pass it your
     * {@link OtelRumConfig} instance. If you would like to "bring your own" SDK, call the
     * two-argument version that takes the SDK as a parameter.
     *
     * @param application The {@link Application} that is being instrumented.
     */
    static OpenTelemetryRumBuilder builder(Application application) {
        return builder(application, new OtelRumConfig());
    }

    /**
     * Returns a new {@link OpenTelemetryRumBuilder} for {@link OpenTelemetryRum} with the given
     * configuration. Use this version if you would like to configure individual aspects of the
     * OpenTelemetry SDK but would still prefer to allow OpenTelemetry RUM to create the SDK for
     * you. If you would like to "bring your own" SDK, call the two-argument version that takes the
     * SDK as a parameter.
     */
    static OpenTelemetryRumBuilder builder(Application application, OtelRumConfig config) {
        return OpenTelemetryRumBuilder.create(application, config);
    }

    /**
     * Returns a new {@link SdkPreconfiguredRumBuilder} for {@link OpenTelemetryRum}. This version
     * requires the user to preconfigure and create their own OpenTelemetrySdk instance. If you
     * prefer to use the builder to configure individual aspects of the OpenTelemetry SDK and to
     * create and manage it for you, call the one-argument version.
     *
     * <p>Specific consideration should be given to the creation of your provided SDK to ensure that
     * the {@link SdkTracerProvider}, {@link SdkMeterProvider}, and {@link SdkLoggerProvider} are
     * configured correctly for your target RUM provider.
     *
     * @param application The {@link Application} that is being instrumented.
     * @param openTelemetrySdk The {@link OpenTelemetrySdk} that the user has already created.
     * @param discoverInstrumentations TRUE to look for instrumentations in the classpath and
     *     applying them automatically.
     */
    static SdkPreconfiguredRumBuilder builder(
            Application application,
            OpenTelemetrySdk openTelemetrySdk,
            boolean discoverInstrumentations) {

        return new SdkPreconfiguredRumBuilder(
                application, openTelemetrySdk, discoverInstrumentations, Services.get(application));
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
