/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.util.Log;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration;
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter;
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.internal.features.networkattrs.NetworkAttributesSpanAppender;
import io.opentelemetry.android.internal.features.persistence.DiskManager;
import io.opentelemetry.android.internal.features.persistence.SimpleTemporaryFileProvider;
import io.opentelemetry.android.internal.initialization.InitializationEvents;
import io.opentelemetry.android.internal.processors.GlobalAttributesLogRecordAppender;
import io.opentelemetry.android.internal.services.CacheStorage;
import io.opentelemetry.android.internal.services.Preferences;
import io.opentelemetry.android.internal.services.ServiceManager;
import io.opentelemetry.android.session.SessionManager;
import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.disk.buffering.LogRecordFromDiskExporter;
import io.opentelemetry.contrib.disk.buffering.LogRecordToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.MetricFromDiskExporter;
import io.opentelemetry.contrib.disk.buffering.MetricToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.SpanFromDiskExporter;
import io.opentelemetry.contrib.disk.buffering.SpanToDiskExporter;
import io.opentelemetry.contrib.disk.buffering.StorageConfiguration;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
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
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A builder of {@link OpenTelemetryRum}. It enabled configuring the OpenTelemetry SDK and disabling
 * built-in Android instrumentations.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class OpenTelemetryRumBuilder {

    private final Application application;
    private final List<BiFunction<SdkTracerProviderBuilder, Application, SdkTracerProviderBuilder>>
            tracerProviderCustomizers = new ArrayList<>();
    private final List<BiFunction<SdkMeterProviderBuilder, Application, SdkMeterProviderBuilder>>
            meterProviderCustomizers = new ArrayList<>();
    private final List<BiFunction<SdkLoggerProviderBuilder, Application, SdkLoggerProviderBuilder>>
            loggerProviderCustomizers = new ArrayList<>();
    private final OtelRumConfig config;
    private final List<AndroidInstrumentation> instrumentations = new ArrayList<>();
    private final List<Consumer<OpenTelemetrySdk>> otelSdkReadyListeners = new ArrayList<>();
    private final SessionIdTimeoutHandler timeoutHandler;
    private SpanExporter spanExporter;
    private LogRecordExporter logRecordExporter;
    private MetricExporter metricExporter;
    private Function<? super TextMapPropagator, ? extends TextMapPropagator> propagatorCustomizer =
            (a) -> a;
    private Supplier<Attributes> globalAttributesSupplier = Attributes::empty;
    private DiskBufferingConfiguration diskBufferingConfiguration =
            DiskBufferingConfiguration.builder().build();

    private Resource resource;

    private static TextMapPropagator buildDefaultPropagator() {
        return TextMapPropagator.composite(
                W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance());
    }

    public static OpenTelemetryRumBuilder create(Application application, OtelRumConfig config) {
        return new OpenTelemetryRumBuilder(
                application, config, new SessionIdTimeoutHandler(config.getSessionTimeout()));
    }

    OpenTelemetryRumBuilder(
            Application application, OtelRumConfig config, SessionIdTimeoutHandler timeoutHandler) {
        this.application = application;
        this.timeoutHandler = timeoutHandler;
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
     * Configures the set of global attributes to emit with every span and event. Any existing
     * configured attributes will be dropped. Default = none.
     */
    public OpenTelemetryRumBuilder setGlobalAttributes(Attributes attributes) {
        return setGlobalAttributes(() -> attributes);
    }

    public OpenTelemetryRumBuilder setGlobalAttributes(
            Supplier<Attributes> globalAttributesSupplier) {
        this.globalAttributesSupplier = globalAttributesSupplier;
        return this;
    }

    private boolean hasGlobalAttributes() {
        Attributes attributes = globalAttributesSupplier.get();
        return attributes != null && !attributes.isEmpty();
    }

    /**
     * Sets the parameters for caching signals in disk in order to export them later.
     *
     * @return this
     */
    public OpenTelemetryRumBuilder setDiskBufferingConfiguration(
            DiskBufferingConfiguration diskBufferingConfiguration) {
        this.diskBufferingConfiguration = diskBufferingConfiguration;
        return this;
    }

    public void setSpanExporter(SpanExporter spanExporter) {
        this.spanExporter = spanExporter;
    }

    public void setLogRecordExporter(LogRecordExporter logRecordExporter) {
        this.logRecordExporter = logRecordExporter;
    }

    public void setMetricExporter(MetricExporter metricExporter) {
        this.metricExporter = metricExporter;
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
        return build(ServiceManager.get());
    }

    OpenTelemetryRum build(ServiceManager serviceManager) {
        InitializationEvents initializationEvents = InitializationEvents.get();
        applyConfiguration(serviceManager, initializationEvents);

        SpanExporter spanExporter = this.spanExporter;
        LogRecordExporter logsExporter = this.logRecordExporter;
        MetricExporter metricExporter = this.metricExporter;
        SignalFromDiskExporter signalFromDiskExporter = null;
        if (diskBufferingConfiguration.isEnabled()) {
            try {
                StorageConfiguration storageConfiguration =
                        createStorageConfiguration(serviceManager);
                final SpanExporter originalSpanExporter = spanExporter;
                spanExporter =
                        SpanToDiskExporter.create(originalSpanExporter, storageConfiguration);
                final LogRecordExporter originalLogsExporter = logsExporter;
                logsExporter =
                        LogRecordToDiskExporter.create(originalLogsExporter, storageConfiguration);
                final MetricExporter originalMetricExporter = metricExporter;
                if (originalMetricExporter != null) {
                    metricExporter =
                            MetricToDiskExporter.create(
                                    metricExporter,
                                    storageConfiguration,
                                    metricExporter::getAggregationTemporality);
                }
                signalFromDiskExporter =
                        new SignalFromDiskExporter(
                                SpanFromDiskExporter.create(
                                        originalSpanExporter, storageConfiguration),
                                (originalMetricExporter != null)
                                        ? MetricFromDiskExporter.create(
                                                originalMetricExporter, storageConfiguration)
                                        : null,
                                LogRecordFromDiskExporter.create(
                                        originalLogsExporter, storageConfiguration));
            } catch (IOException e) {
                Log.e(RumConstants.OTEL_RUM_LOG_TAG, "Could not initialize disk exporters.", e);
            }
        }
        initializationEvents.spanExporterInitialized(spanExporter);

        SessionManager sessionManager =
                SessionManager.create(timeoutHandler, config.getSessionTimeout().toNanos());

        OpenTelemetrySdk sdk =
                OpenTelemetrySdk.builder()
                        .setTracerProvider(
                                buildTracerProvider(sessionManager, application, spanExporter))
                        .setMeterProvider(buildMeterProvider(application, metricExporter))
                        .setLoggerProvider(buildLoggerProvider(application, logsExporter))
                        .setPropagators(buildFinalPropagators())
                        .build();

        otelSdkReadyListeners.forEach(listener -> listener.accept(sdk));

        scheduleDiskTelemetryReader(signalFromDiskExporter, diskBufferingConfiguration);

        SdkPreconfiguredRumBuilder delegate =
                new SdkPreconfiguredRumBuilder(
                        application,
                        sdk,
                        timeoutHandler,
                        sessionManager,
                        config.shouldDiscoverInstrumentations(),
                        serviceManager);
        instrumentations.forEach(delegate::addInstrumentation);
        return delegate.build();
    }

    private StorageConfiguration createStorageConfiguration(ServiceManager serviceManager)
            throws IOException {
        Preferences preferences = serviceManager.getPreferences();
        CacheStorage storage = serviceManager.getCacheStorage();
        DiskManager diskManager = new DiskManager(storage, preferences, diskBufferingConfiguration);
        return StorageConfiguration.builder()
                .setRootDir(diskManager.getSignalsBufferDir())
                .setMaxFileSize(diskManager.getMaxCacheFileSize())
                .setMaxFolderSize(diskManager.getMaxFolderSize())
                .setMaxFileAgeForWriteMillis(
                        diskBufferingConfiguration.getMaxFileAgeForWriteMillis())
                .setMaxFileAgeForReadMillis(diskBufferingConfiguration.getMaxFileAgeForReadMillis())
                .setMinFileAgeForReadMillis(diskBufferingConfiguration.getMinFileAgeForReadMillis())
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

    /**
     * Adds a callback to be invoked after the OpenTelemetry SDK has been initialized. This can be
     * used to defer some early lifecycle functionality until the working SDK is ready.
     *
     * @param callback - A callback that receives the OpenTelemetry SDK instance.
     * @return this
     */
    public OpenTelemetryRumBuilder addOtelSdkReadyListener(Consumer<OpenTelemetrySdk> callback) {
        otelSdkReadyListeners.add(callback);
        return this;
    }

    /** Leverage the configuration to wire up various instrumentation components. */
    private void applyConfiguration(
            ServiceManager serviceManager, InitializationEvents initializationEvents) {
        if (config.shouldGenerateSdkInitializationEvents()) {
            initializationEvents.recordConfiguration(config);
        }
        initializationEvents.sdkInitializationStarted();

        // Global attributes
        if (hasGlobalAttributes()) {
            // Add span processor that appends global attributes.
            GlobalAttributesSpanAppender appender =
                    GlobalAttributesSpanAppender.create(globalAttributesSupplier);
            addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) ->
                            tracerProviderBuilder.addSpanProcessor(appender));
        }

        // Network specific attributes
        if (config.shouldIncludeNetworkAttributes()) {
            // Add span processor that appends network attributes.
            addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) -> {
                        SpanProcessor networkAttributesSpanAppender =
                                NetworkAttributesSpanAppender.create(
                                        serviceManager.getCurrentNetworkProvider());
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
                                        serviceManager.getVisibleScreenService());
                        return tracerProviderBuilder.addSpanProcessor(screenAttributesAppender);
                    });
        }
    }

    private SdkTracerProvider buildTracerProvider(
            SessionProvider sessionProvider, Application application, SpanExporter spanExporter) {
        SdkTracerProviderBuilder tracerProviderBuilder =
                SdkTracerProvider.builder()
                        .setResource(resource)
                        .addSpanProcessor(new SessionIdSpanAppender(sessionProvider));

        BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(spanExporter).build();
        tracerProviderBuilder.addSpanProcessor(batchSpanProcessor);

        for (BiFunction<SdkTracerProviderBuilder, Application, SdkTracerProviderBuilder>
                customizer : tracerProviderCustomizers) {
            tracerProviderBuilder = customizer.apply(tracerProviderBuilder, application);
        }
        return tracerProviderBuilder.build();
    }

    private SdkLoggerProvider buildLoggerProvider(
            Application application, LogRecordExporter logsExporter) {
        SdkLoggerProviderBuilder loggerProviderBuilder =
                SdkLoggerProvider.builder()
                        .addLogRecordProcessor(
                                new GlobalAttributesLogRecordAppender(globalAttributesSupplier))
                        .setResource(resource);
        LogRecordProcessor batchLogsProcessor =
                BatchLogRecordProcessor.builder(logsExporter).build();
        loggerProviderBuilder.addLogRecordProcessor(batchLogsProcessor);
        for (BiFunction<SdkLoggerProviderBuilder, Application, SdkLoggerProviderBuilder>
                customizer : loggerProviderCustomizers) {
            loggerProviderBuilder = customizer.apply(loggerProviderBuilder, application);
        }
        return loggerProviderBuilder.build();
    }

    private SdkMeterProvider buildMeterProvider(
            Application application, @Nullable MetricExporter metricExporter) {
        SdkMeterProviderBuilder meterProviderBuilder =
                SdkMeterProvider.builder().setResource(resource);
        if (metricExporter != null) {
            meterProviderBuilder.registerMetricReader(PeriodicMetricReader.create(metricExporter));
        }
        for (BiFunction<SdkMeterProviderBuilder, Application, SdkMeterProviderBuilder> customizer :
                meterProviderCustomizers) {
            meterProviderBuilder = customizer.apply(meterProviderBuilder, application);
        }
        return meterProviderBuilder.build();
    }

    private ContextPropagators buildFinalPropagators() {
        TextMapPropagator defaultPropagator = buildDefaultPropagator();
        return ContextPropagators.create(propagatorCustomizer.apply(defaultPropagator));
    }
}
