/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import android.app.Application;
import io.opentelemetry.android.instrumentation.InstrumentedApplication;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A builder of {@link OpenTelemetryRum}. It enabled configuring the OpenTelemetry SDK and disabling
 * built-in Android instrumentations.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class OpenTelemetryRumBuilder {

    private final SessionId sessionId;
    private final Application application;
    private final List<BiFunction<SdkTracerProviderBuilder, Application, SdkTracerProviderBuilder>>
            tracerProviderCustomizers = new ArrayList<>();
    private final List<BiFunction<SdkMeterProviderBuilder, Application, SdkMeterProviderBuilder>>
            meterProviderCustomizers = new ArrayList<>();
    private final List<BiFunction<SdkLoggerProviderBuilder, Application, SdkLoggerProviderBuilder>>
            loggerProviderCustomizers = new ArrayList<>();
    private final List<Consumer<InstrumentedApplication>> instrumentationInstallers =
            new ArrayList<>();

    private final Map<Class<? extends TextMapPropagator>, TextMapPropagator> propagators =
            buildDefaultPropagators();

    private Resource resource;

    private static Map<Class<? extends TextMapPropagator>, TextMapPropagator>
            buildDefaultPropagators() {
        Map<Class<? extends TextMapPropagator>, TextMapPropagator> result = new HashMap<>();
        putPropagator(result, W3CTraceContextPropagator.getInstance());
        putPropagator(result, W3CBaggagePropagator.getInstance());
        return result;
    }

    private static void putPropagator(
            Map<Class<? extends TextMapPropagator>, TextMapPropagator> propagators,
            TextMapPropagator propagator) {
        propagators.put(propagator.getClass(), propagator);
    }

    OpenTelemetryRumBuilder(Application application) {
        this.application = application;
        SessionIdTimeoutHandler timeoutHandler = new SessionIdTimeoutHandler();
        this.sessionId = new SessionId(timeoutHandler);
        this.resource = AndroidResource.createDefault(application);
    }

    /**
     * Assign a {@link Resource} to be attached to all telemetry emitted by the {@link
     * OpenTelemetryRum} created by this builder. This replaces any existing resource.
     *
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder setResource(Resource resource) {
        this.resource = resource;
        return this;
    }

    /**
     * Merges a new {@link Resource} with any existing {@link Resource} in this builder. The
     * resulting {@link Resource} will be attached to all telemetry emitted by the {@link
     * OpenTelemetryRum} created by this builder.
     *
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder mergeResource(Resource resource) {
        this.resource = this.resource.merge(resource);
        return this;
    }

    /**
     * Adds a {@link BiFunction} to invoke the with the {@link SdkTracerProviderBuilder} to allow
     * customization. The return value of the {@link BiFunction} will replace the passed-in
     * argument.
     *
     * <p>Multiple calls will execute the customizers in order.
     *
     * <p>Note: calling {@link SdkTracerProviderBuilder#setResource(Resource)} inside of your
     * configuration function will cause any resource customizers to be ignored that were configured
     * via {@link #setResource(Resource)}.
     *
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder addTracerProviderCustomizer(
            BiFunction<SdkTracerProviderBuilder, Application, SdkTracerProviderBuilder>
                    customizer) {
        tracerProviderCustomizers.add(customizer);
        return this;
    }

    /**
     * Adds a {@link BiFunction} to invoke the with the {@link SdkMeterProviderBuilder} to allow
     * customization. The return value of the {@link BiFunction} will replace the passed-in
     * argument.
     *
     * <p>Multiple calls will execute the customizers in order.
     *
     * <p>Note: calling {@link SdkMeterProviderBuilder#setResource(Resource)} inside of your
     * configuration function will cause any resource customizers to be ignored that were configured
     * via {@link #setResource(Resource)}.
     *
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder addMeterProviderCustomizer(
            BiFunction<SdkMeterProviderBuilder, Application, SdkMeterProviderBuilder> customizer) {
        meterProviderCustomizers.add(customizer);
        return this;
    }

    /**
     * Adds a {@link BiFunction} to invoke the with the {@link SdkLoggerProviderBuilder} to allow
     * customization. The return value of the {@link BiFunction} will replace the passed-in
     * argument.
     *
     * <p>Multiple calls will execute the customizers in order.
     *
     * <p>Note: calling {@link SdkLoggerProviderBuilder#setResource(Resource)} inside of your
     * configuration function will cause any resource customizers to be ignored that were configured
     * via {@link #setResource(Resource)}.
     *
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder addLoggerProviderCustomizer(
            BiFunction<SdkLoggerProviderBuilder, Application, SdkLoggerProviderBuilder>
                    customizer) {
        loggerProviderCustomizers.add(customizer);
        return this;
    }

    /**
     * Adds an instrumentation installer function that will be run on an {@link
     * InstrumentedApplication} instance as a part of the {@link #build()} method call.
     *
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder addInstrumentation(
            Consumer<InstrumentedApplication> instrumentationInstaller) {
        instrumentationInstallers.add(instrumentationInstaller);
        return this;
    }

    /**
     * Adds a new propagator to be used by the OpenTelemetry SDK. Calling this method will not
     * remove any existing propagators, including the defaults: W3CTraceContextPropagator and
     * W3CBaggagePropagator. For complete control over which propagators are used, call {@code
     * setPropagator(TextMapPropagator propagator)}.
     *
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder addPropagator(TextMapPropagator propagator) {
        putPropagator(propagators, propagator);
        return this;
    }

    /**
     * This removes any existing propagators and configures a new one to by used by the
     * OpenTelemetry SDK. If you need to configure more than one propagator, you should use {@code
     * TextMapPropagator.composite()} and pass the composite propagator to this method.
     *
     * @param propagator The new TextMapPropagator to use when configuring the otel SDK.
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder setPropagator(TextMapPropagator propagator) {
        propagators.clear();
        putPropagator(propagators, propagator);
        return this;
    }

    public SessionId getSessionId() {
        return sessionId;
    }

    /**
     * Creates a new instance of {@link OpenTelemetryRum} with the settings of this {@link
     * OpenTelemetryRumBuilder}.
     *
     * <p>This method will initialize the OpenTelemetry SDK and install built-in system
     * instrumentations in the passed Android {@link Application}.
     *
     * @return A new {@link OpenTelemetryRum} instance.
     */
    public OpenTelemetryRum build() {
        OpenTelemetrySdk sdk =
                OpenTelemetrySdk.builder()
                        .setTracerProvider(buildTracerProvider(sessionId, application))
                        .setMeterProvider(buildMeterProvider(application))
                        .setLoggerProvider(buildLoggerProvider(application))
                        .setPropagators(buildFinalPropagators())
                        .build();

        SdkPreconfiguredRumBuilder delegate =
                new SdkPreconfiguredRumBuilder(application, sdk, sessionId);
        instrumentationInstallers.forEach(delegate::addInstrumentation);
        return delegate.build();
    }

    private SdkTracerProvider buildTracerProvider(SessionId sessionId, Application application) {
        SdkTracerProviderBuilder tracerProviderBuilder =
                SdkTracerProvider.builder()
                        .setResource(resource)
                        .addSpanProcessor(new SessionIdSpanAppender(sessionId));
        for (BiFunction<SdkTracerProviderBuilder, Application, SdkTracerProviderBuilder>
                customizer : tracerProviderCustomizers) {
            tracerProviderBuilder = customizer.apply(tracerProviderBuilder, application);
        }
        return tracerProviderBuilder.build();
    }

    private SdkMeterProvider buildMeterProvider(Application application) {
        SdkMeterProviderBuilder meterProviderBuilder =
                SdkMeterProvider.builder().setResource(resource);
        for (BiFunction<SdkMeterProviderBuilder, Application, SdkMeterProviderBuilder> customizer :
                meterProviderCustomizers) {
            meterProviderBuilder = customizer.apply(meterProviderBuilder, application);
        }
        return meterProviderBuilder.build();
    }

    private SdkLoggerProvider buildLoggerProvider(Application application) {
        SdkLoggerProviderBuilder loggerProviderBuilder =
                SdkLoggerProvider.builder().setResource(resource);
        for (BiFunction<SdkLoggerProviderBuilder, Application, SdkLoggerProviderBuilder>
                customizer : loggerProviderCustomizers) {
            loggerProviderBuilder = customizer.apply(loggerProviderBuilder, application);
        }
        return loggerProviderBuilder.build();
    }

    private ContextPropagators buildFinalPropagators() {
        return ContextPropagators.create(TextMapPropagator.composite(propagators.values()));
    }
}
