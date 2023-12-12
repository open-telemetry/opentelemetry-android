/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.util.Log;
import io.opentelemetry.android.config.DiskBufferingConfiguration;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.instrumentation.InstrumentedApplication;
import io.opentelemetry.android.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.android.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.android.instrumentation.network.NetworkAttributesSpanAppender;
import io.opentelemetry.android.instrumentation.network.NetworkChangeMonitor;
import io.opentelemetry.android.instrumentation.startup.InitializationEvents;
import io.opentelemetry.android.instrumentation.startup.SdkInitializationEvents;
import io.opentelemetry.android.internal.features.persistence.DiskManager;
import io.opentelemetry.android.internal.features.persistence.SimpleTemporaryFileProvider;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.disk.buffering.SpanDiskExporter;
import io.opentelemetry.contrib.disk.buffering.internal.StorageConfiguration;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;

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
    private final OtelRumConfig config;
    private final VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();

    private Function<? super SpanExporter, ? extends SpanExporter> spanExporterCustomizer = a -> a;
    private final List<Consumer<InstrumentedApplication>> instrumentationInstallers =
            new ArrayList<>();

    private Function<? super TextMapPropagator, ? extends TextMapPropagator> propagatorCustomizer =
            (a) -> a;

    private Resource resource;
    @Nullable private CurrentNetworkProvider currentNetworkProvider = null;
    private InitializationEvents initializationEvents = InitializationEvents.NO_OP;

    private static TextMapPropagator buildDefaultPropagator() {
        return TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance());
    }

    OpenTelemetryRumBuilder(Application application, OtelRumConfig config) {
        this.application = application;
        SessionIdTimeoutHandler timeoutHandler = new SessionIdTimeoutHandler();
        this.sessionId = new SessionId(timeoutHandler);
        this.resource = AndroidResource.createDefault(application);
        this.config = config;
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
     * Call this to pass an existing CurrentNetworkProvider instance to share with the underlying
     * OpenTelemetry Rum instrumentation.
     *
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder setCurrentNetworkProvider(
            CurrentNetworkProvider currentNetworkProvider) {
        this.currentNetworkProvider = currentNetworkProvider;
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
     * Adds a {@link Function} to invoke with the default {@link TextMapPropagator} to allow
     * customization. The return value of the {@link BiFunction} will replace the passed-in
     * argument. To add new propagators, use {@code TextMapPropagator.composite()} with the existing
     * propagator passed to your function.
     *
     * <p>Multiple calls will execute the customizers in order.
     */
    public OpenTelemetryRumBuilder addPropagatorCustomizer(
            Function<? super TextMapPropagator, ? extends TextMapPropagator> propagatorCustomizer) {
        requireNonNull(propagatorCustomizer, "propagatorCustomizer");
        Function<? super TextMapPropagator, ? extends TextMapPropagator> existing =
                this.propagatorCustomizer;
        this.propagatorCustomizer =
                propagator -> {
                    TextMapPropagator result = existing.apply(propagator);
                    return propagatorCustomizer.apply(result);
                };
        return this;
    }

    /**
     * Adds a {@link Function} to invoke with the default {@link SpanExporter} to allow
     * customization. The return value of the {@link Function} will replace the passed-in argument.
     *
     * <p>Multiple calls will execute the customizers in order.
     */
    public OpenTelemetryRumBuilder addSpanExporterCustomizer(
            Function<? super SpanExporter, ? extends SpanExporter> spanExporterCustomizer) {
        requireNonNull(spanExporterCustomizer, "spanExporterCustomizer");
        Function<? super SpanExporter, ? extends SpanExporter> existing = spanExporterCustomizer;
        this.spanExporterCustomizer =
                exporter -> {
                    SpanExporter intermediate = existing.apply(exporter);
                    return spanExporterCustomizer.apply(intermediate);
                };
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

        applyConfiguration();

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

    /** Leverage the configuration to wire up various instrumentation components. */
    private void applyConfiguration() {
        if (config.shouldGenerateSdkInitializationEvents()) {
            initializationEvents = new SdkInitializationEvents();
            initializationEvents.recordConfiguration(config);
        }
        initializationEvents.sdkInitializationStarted();

        // Global attributes
        if (config.hasGlobalAttributes()) {
            // Add span processor that appends global attributes.
            GlobalAttributesSpanAppender appender =
                    GlobalAttributesSpanAppender.create(config.getGlobalAttributesSupplier());
            addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) ->
                            tracerProviderBuilder.addSpanProcessor(appender));
        }

        // Network specific attributes
        if (config.shouldIncludeNetworkAttributes()) {
            // Add span processor that appends network attributes.
            CurrentNetworkProvider currentNetworkProvider = getOrCreateCurrentNetworkProvider();
            addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) -> {
                        SpanProcessor networkAttributesSpanAppender =
                                NetworkAttributesSpanAppender.create(currentNetworkProvider);
                        return tracerProviderBuilder.addSpanProcessor(
                                networkAttributesSpanAppender);
                    });
            initializationEvents.currentNetworkProviderInitialized();
        }

        // Add network change monitor if enabled (default = = true)
        if (config.isNetworkChangeMonitoringEnabled()) {
            addInstrumentation(
                    app -> {
                        NetworkChangeMonitor.create(getOrCreateCurrentNetworkProvider())
                                .installOn(app);
                        initializationEvents.networkMonitorInitialized();
                    });
        }

        // Add span processor that appends screen attribute(s)
        if (config.shouldIncludeScreenAttributes()) {
            addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) -> {
                        SpanProcessor screenAttributesAppender =
                                new ScreenAttributesSpanProcessor(visibleScreenTracker);
                        return tracerProviderBuilder.addSpanProcessor(screenAttributesAppender);
                    });
        }
    }

    private CurrentNetworkProvider getOrCreateCurrentNetworkProvider() {
        if (currentNetworkProvider == null) {
            this.currentNetworkProvider = CurrentNetworkProvider.createAndStart(application);
        }
        return currentNetworkProvider;
    }

    private SdkTracerProvider buildTracerProvider(SessionId sessionId, Application application) {
        SdkTracerProviderBuilder tracerProviderBuilder =
                SdkTracerProvider.builder()
                        .setResource(resource)
                        .addSpanProcessor(new SessionIdSpanAppender(sessionId));

        SpanExporter spanExporter = buildSpanExporter();
        BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(spanExporter).build();
        tracerProviderBuilder.addSpanProcessor(batchSpanProcessor);

        for (BiFunction<SdkTracerProviderBuilder, Application, SdkTracerProviderBuilder>
                customizer : tracerProviderCustomizers) {
            tracerProviderBuilder = customizer.apply(tracerProviderBuilder, application);
        }
        return tracerProviderBuilder.build();
    }

    private SpanExporter buildSpanExporter() {
        // TODO: Default to otlp...but how can we make endpoint and auth mandatory?
        SpanExporter defaultExporter = LoggingSpanExporter.create();
        SpanExporter spanExporter = defaultExporter;
        DiskBufferingConfiguration diskBufferingConfiguration =
                config.getDiskBufferingConfiguration();
        if (diskBufferingConfiguration.isEnabled()) {
            try {
                spanExporter = createDiskExporter(defaultExporter, diskBufferingConfiguration);
            } catch (IOException e) {
                Log.w(RumConstants.OTEL_RUM_LOG_TAG, "Could not create span disk exporter.", e);
            }
        }
        return spanExporterCustomizer.apply(spanExporter);
    }

    private static SpanExporter createDiskExporter(
            SpanExporter defaultExporter, DiskBufferingConfiguration diskBufferingConfiguration)
            throws IOException {
        DiskManager diskManager = DiskManager.create(diskBufferingConfiguration);
        StorageConfiguration storageConfiguration =
                StorageConfiguration.builder()
                        .setMaxFileSize(diskManager.getMaxCacheFileSize())
                        .setMaxFolderSize(diskManager.getMaxFolderSize())
                        .setTemporaryFileProvider(
                                new SimpleTemporaryFileProvider(diskManager.getTemporaryDir()))
                        .build();
        return SpanDiskExporter.create(
                defaultExporter, diskManager.getSignalsBufferDir(), storageConfiguration);
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
        TextMapPropagator defaultPropagator = buildDefaultPropagator();
        return ContextPropagators.create(propagatorCustomizer.apply(defaultPropagator));
    }
}
