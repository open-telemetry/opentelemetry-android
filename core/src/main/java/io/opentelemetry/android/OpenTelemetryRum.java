/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import android.app.Application;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

/** Entrypoint for the OpenTelemetry Real User Monitoring library for Android. */
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
     * @param config The {@link OtelRumConfig} instance.
     * @param sessionProvider The {@link SessionProvider} instance.
     */
    static SdkPreconfiguredRumBuilder builder(
            Application application,
            OpenTelemetrySdk openTelemetrySdk,
            OtelRumConfig config,
            SessionProvider sessionProvider) {
        return new SdkPreconfiguredRumBuilder(
                application, openTelemetrySdk, sessionProvider, config);
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

    /**
     * Emits an event with the specified name.
     *
     * <p>This method serves as a convenience overload that emits an event with an empty set of
     * attributes.
     *
     * @param eventName The name of the event to emit.
     */
    default void emitEvent(String eventName) {
        emitEvent(eventName, Attributes.empty());
    }

    /**
     * Emits an event with the specified name and body.
     *
     * <p>This method serves as a convenience overload that emits an event with an empty set of
     * attributes.
     *
     * @param eventName The name of the event to emit.
     * @param body The body of the event, typically containing additional data.
     */
    default void emitEvent(String eventName, String body) {
        emitEvent(eventName, body, Attributes.empty());
    }

    /**
     * Emits an event with the specified name and attributes.
     *
     * <p>This method serves as a convenience overload that emits an event with an empty body.
     *
     * @param eventName The name of the event to emit.
     * @param attributes The attributes associated with the event.
     */
    default void emitEvent(String eventName, Attributes attributes) {
        emitEvent(eventName, "", attributes);
    }

    /**
     * Emits an event with the specified name, body, and attributes.
     *
     * <p>Implementations of this method should define how the event is handled and recorded.
     *
     * @param eventName The name of the event to emit.
     * @param body The body of the event, typically containing additional data.
     * @param attributes The attributes associated with the event, providing metadata.
     */
    void emitEvent(String eventName, String body, Attributes attributes);

    /**
     * Initiates orderly shutdown of this OpenTelemetryRum instance. After this method completes,
     * the instance should be considered invalid and no longer used.
     */
    void shutdown();
}
