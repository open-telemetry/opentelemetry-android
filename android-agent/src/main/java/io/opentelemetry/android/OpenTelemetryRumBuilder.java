/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.util.Log;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration;
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter;
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler;
import io.opentelemetry.android.features.networkattrs.CurrentNetworkProvider;
import io.opentelemetry.android.features.networkattrs.NetworkAttributesSpanAppender;
import io.opentelemetry.android.features.screenattrs.ScreenAttributesSpanProcessor;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.internal.features.persistence.DiskManager;
import io.opentelemetry.android.internal.features.persistence.SimpleTemporaryFileProvider;
import io.opentelemetry.android.internal.processors.GlobalAttributesLogRecordAppender;
import io.opentelemetry.android.internal.services.ServiceManager;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService;
import io.opentelemetry.android.internal.tools.RumConstants;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.disk.buffering.SpanFromDiskExporter;
import io.opentelemetry.contrib.disk.buffering.SpanToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.StorageConfiguration;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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

    private Function<? super SpanExporter, ? extends SpanExporter> spanExporterCustomizer = a -> a;
    private final List<AndroidInstrumentation> instrumentations = new ArrayList<>();

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
     * Adds an instrumentation to be applied as a part of the {@link #build()} method call.
     *
     * @return {@code this}
     */
    public OpenTelemetryRumBuilder addInstrumentation(AndroidInstrumentation instrumentation) {
        instrumentations.add(instrumentation);
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
        ServiceManager.initialize(application);

        applyConfiguration();

        DiskBufferingConfiguration diskBufferingConfiguration =
                config.getDiskBufferingConfiguration();
        SpanExporter spanExporter = buildSpanExporter();
        SignalFromDiskExporter signalFromDiskExporter = null;
        if (diskBufferingConfiguration.isEnabled()) {
            try {
                StorageConfiguration storageConfiguration = createStorageConfiguration();
                final SpanExporter originalSpanExporter = spanExporter;
                spanExporter =
                        SpanToDiskExporter.create(originalSpanExporter, storageConfiguration);

                signalFromDiskExporter =
                        new SignalFromDiskExporter(
                                SpanFromDiskExporter.create(
                                        originalSpanExporter, storageConfiguration),
                                null,
                                null);
            } catch (IOException e) {
                Log.e(RumConstants.OTEL_RUM_LOG_TAG, "Could not initialize disk exporters.", e);
            }
        }

        OpenTelemetrySdk sdk =
                OpenTelemetrySdk.builder()
                        .setTracerProvider(
                                buildTracerProvider(sessionId, application, spanExporter))
                        .setMeterProvider(buildMeterProvider(application))
                        .setLoggerProvider(buildLoggerProvider(application))
                        .setPropagators(buildFinalPropagators())
                        .build();

        scheduleDiskTelemetryReader(signalFromDiskExporter, diskBufferingConfiguration);

        SdkPreconfiguredRumBuilder delegate =
                new SdkPreconfiguredRumBuilder(application, sdk, sessionId);
        instrumentations.forEach(delegate::addInstrumentation);
        ServiceManager.get().start();
        return delegate.build();
    }

    private StorageConfiguration createStorageConfiguration() throws IOException {
        DiskManager diskManager = DiskManager.create(config.getDiskBufferingConfiguration());
        return StorageConfiguration.builder()
                .setMaxFileSize(diskManager.getMaxCacheFileSize())
                .setMaxFolderSize(diskManager.getMaxFolderSize())
                .setRootDir(diskManager.getSignalsBufferDir())
                .setTemporaryFileProvider(
                        new SimpleTemporaryFileProvider(diskManager.getTemporaryDir()))
                .build();
    }

    private void scheduleDiskTelemetryReader(
            @Nullable SignalFromDiskExporter signalExporter,
            DiskBufferingConfiguration diskBufferingConfiguration) {
        ExportScheduleHandler exportScheduleHandler =
                diskBufferingConfiguration.getExportScheduleHandler();
        if (signalExporter == null) {
            // Disabling here allows to cancel previously scheduled exports using tools that
            // can run even after the app has been terminated (such as WorkManager).
            // But for in-memory only schedulers, nothing should need to be disabled.
            exportScheduleHandler.disable();
        } else {
            // Not null means that disk buffering is enabled and disk exporters are successfully
            // initialized.
            SignalFromDiskExporter.set(signalExporter);
            exportScheduleHandler.enable();
        }
    }

    /** Leverage the configuration to wire up various instrumentation components. */
    private void applyConfiguration() {
        if (config.shouldGenerateSdkInitializationEvents()) {
            if (initializationEvents == InitializationEvents.NO_OP) {
                initializationEvents = new SdkInitializationEvents();
            }
            Map<String, String> configMap = new HashMap<>();
            // TODO: Convert config to map
            initializationEvents.recordConfiguration(configMap);
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

        // Add span processor that appends screen attribute(s)
        if (config.shouldIncludeScreenAttributes()) {
            addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) -> {
                        SpanProcessor screenAttributesAppender =
                                new ScreenAttributesSpanProcessor(
                                        ServiceManager.get()
                                                .getService(VisibleScreenService.class));
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

    private SdkTracerProvider buildTracerProvider(
            SessionId sessionId, Application application, SpanExporter spanExporter) {
        SdkTracerProviderBuilder tracerProviderBuilder =
                SdkTracerProvider.builder()
                        .setResource(resource)
                        .addSpanProcessor(new SessionIdSpanAppender(sessionId));

        initializationEvents.spanExporterInitialized(spanExporter);
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
        return spanExporterCustomizer.apply(defaultExporter);
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
                SdkLoggerProvider.builder()
                        .addLogRecordProcessor(
                                new GlobalAttributesLogRecordAppender(
                                        config.getGlobalAttributesSupplier()))
                        .setResource(resource);
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

    OpenTelemetryRumBuilder setInitializationEvents(InitializationEvents initializationEvents) {
        this.initializationEvents = initializationEvents;
        return this;
    }
}
