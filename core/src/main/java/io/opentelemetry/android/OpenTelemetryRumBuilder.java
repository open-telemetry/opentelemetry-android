/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.export.BufferDelegatingLogExporter;
import io.opentelemetry.android.export.BufferDelegatingMetricExporter;
import io.opentelemetry.android.export.BufferDelegatingSpanExporter;
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig;
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter;
import io.opentelemetry.android.features.diskbuffering.scheduler.DefaultExportScheduleHandler;
import io.opentelemetry.android.features.diskbuffering.scheduler.DefaultExportScheduler;
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.internal.features.networkattrs.NetworkAttributesSpanAppender;
import io.opentelemetry.android.internal.features.persistence.DiskManager;
import io.opentelemetry.android.internal.features.persistence.SimpleTemporaryFileProvider;
import io.opentelemetry.android.internal.initialization.InitializationEvents;
import io.opentelemetry.android.internal.processors.GlobalAttributesLogRecordAppender;
import io.opentelemetry.android.internal.processors.ScreenAttributesLogRecordProcessor;
import io.opentelemetry.android.internal.processors.SessionIdLogRecordAppender;
import io.opentelemetry.android.internal.services.CacheStorage;
import io.opentelemetry.android.internal.services.Services;
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWork;
import io.opentelemetry.android.internal.session.SessionIdTimeoutHandler;
import io.opentelemetry.android.internal.session.SessionManagerImpl;
import io.opentelemetry.android.session.SessionManager;
import io.opentelemetry.android.session.SessionProvider;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
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
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
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
import kotlin.jvm.functions.Function0;

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
    private Function<? super SpanExporter, ? extends SpanExporter> spanExporterCustomizer = a -> a;
    private Function<? super MetricExporter, ? extends MetricExporter> metricExporterCustomizer =
            a -> a;
    private Function<? super LogRecordExporter, ? extends LogRecordExporter>
            logRecordExporterCustomizer = a -> a;
    private Function<? super TextMapPropagator, ? extends TextMapPropagator> propagatorCustomizer =
            (a) -> a;

    private Resource resource;

    @Nullable private ExportScheduleHandler exportScheduleHandler;
    @Nullable private SessionManager sessionManager;

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
        Function<? super SpanExporter, ? extends SpanExporter> existing =
                this.spanExporterCustomizer;
        this.spanExporterCustomizer =
                exporter -> {
                    SpanExporter intermediate = existing.apply(exporter);
                    return spanExporterCustomizer.apply(intermediate);
                };
        return this;
    }

    /**
     * Adds a {@link Function} to invoke with the default {@link MetricExporter} to allow
     * customization. The return value of the {@link Function} will replace the passed-in argument.
     *
     * <p>Multiple calls will execute the customizers in order.
     */
    public OpenTelemetryRumBuilder addMetricExporterCustomizer(
            Function<? super MetricExporter, ? extends MetricExporter> metricExporterCustomizer) {
        requireNonNull(metricExporterCustomizer, "metricExporterCustomizer");
        Function<? super MetricExporter, ? extends MetricExporter> existing =
                this.metricExporterCustomizer;
        this.metricExporterCustomizer =
                exporter -> {
                    MetricExporter intermediate = existing.apply(exporter);
                    return metricExporterCustomizer.apply(intermediate);
                };
        return this;
    }

    /**
     * Adds a {@link Function} to invoke with the default {@link LogRecordExporter} to allow
     * customization. The return value of the {@link Function} will replace the passed-in argument.
     *
     * <p>Multiple calls will execute the customizers in order.
     */
    public OpenTelemetryRumBuilder addLogRecordExporterCustomizer(
            Function<? super LogRecordExporter, ? extends LogRecordExporter>
                    logRecordExporterCustomizer) {
        Function<? super LogRecordExporter, ? extends LogRecordExporter> existing =
                this.logRecordExporterCustomizer;
        this.logRecordExporterCustomizer =
                exporter -> {
                    LogRecordExporter intermediate = existing.apply(exporter);
                    return logRecordExporterCustomizer.apply(intermediate);
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
        Services services = Services.get(application);
        InitializationEvents initializationEvents = InitializationEvents.get();
        applyConfiguration(services, initializationEvents);

        BufferDelegatingSpanExporter bufferDelegatingSpanExporter =
                new BufferDelegatingSpanExporter();
        BufferDelegatingLogExporter bufferDelegatingLogExporter = new BufferDelegatingLogExporter();
        BufferDelegatingMetricExporter bufferDelegatingMetricExporter =
                new BufferDelegatingMetricExporter();

        if (sessionManager == null) {
            sessionManager =
                    SessionManagerImpl.create(timeoutHandler, config.getSessionTimeout().toNanos());
        }

        OpenTelemetrySdk sdk =
                OpenTelemetrySdk.builder()
                        .setTracerProvider(
                                buildTracerProvider(
                                        sessionManager, application, bufferDelegatingSpanExporter))
                        .setLoggerProvider(
                                buildLoggerProvider(
                                        sessionManager, application, bufferDelegatingLogExporter))
                        .setMeterProvider(
                                buildMeterProvider(application, bufferDelegatingMetricExporter))
                        .setPropagators(buildFinalPropagators())
                        .build();

        otelSdkReadyListeners.forEach(listener -> listener.accept(sdk));

        SdkPreconfiguredRumBuilder delegate =
                new SdkPreconfiguredRumBuilder(
                        application,
                        sdk,
                        timeoutHandler,
                        sessionManager,
                        config.shouldDiscoverInstrumentations(),
                        services);

        // AsyncTask is deprecated but the thread pool is still used all over the Android SDK
        // and it provides a way to get a background thread without having to create a new one.
        android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute(
                () ->
                        initializeExporters(
                                services,
                                initializationEvents,
                                bufferDelegatingSpanExporter,
                                bufferDelegatingLogExporter,
                                bufferDelegatingMetricExporter));

        instrumentations.forEach(delegate::addInstrumentation);

        return delegate.build();
    }

    private void initializeExporters(
            Services services,
            InitializationEvents initializationEvents,
            BufferDelegatingSpanExporter bufferDelegatingSpanExporter,
            BufferDelegatingLogExporter bufferedDelegatingLogExporter,
            BufferDelegatingMetricExporter bufferDelegatingMetricExporter) {

        DiskBufferingConfig diskBufferingConfig = config.getDiskBufferingConfig();
        SpanExporter spanExporter = buildSpanExporter();
        LogRecordExporter logsExporter = buildLogsExporter();
        MetricExporter metricExporter = buildMetricExporter();
        SignalFromDiskExporter signalFromDiskExporter = null;
        if (diskBufferingConfig.getEnabled()) {
            try {
                StorageConfiguration storageConfiguration = createStorageConfiguration(services);
                final SpanExporter originalSpanExporter = spanExporter;
                spanExporter =
                        SpanToDiskExporter.create(originalSpanExporter, storageConfiguration);
                final LogRecordExporter originalLogsExporter = logsExporter;
                logsExporter =
                        LogRecordToDiskExporter.create(originalLogsExporter, storageConfiguration);
                final MetricExporter originalMetricExporter = metricExporter;
                metricExporter =
                        MetricToDiskExporter.create(originalMetricExporter, storageConfiguration);
                signalFromDiskExporter =
                        new SignalFromDiskExporter(
                                SpanFromDiskExporter.create(
                                        originalSpanExporter, storageConfiguration),
                                MetricFromDiskExporter.create(
                                        originalMetricExporter, storageConfiguration),
                                LogRecordFromDiskExporter.create(
                                        originalLogsExporter, storageConfiguration));
            } catch (IOException e) {
                Log.e(RumConstants.OTEL_RUM_LOG_TAG, "Could not initialize disk exporters.", e);
            }
        }
        initializationEvents.spanExporterInitialized(spanExporter);
        bufferedDelegatingLogExporter.setDelegate(logsExporter);
        bufferDelegatingSpanExporter.setDelegate(spanExporter);
        bufferDelegatingMetricExporter.setDelegate(metricExporter);
        scheduleDiskTelemetryReader(services, signalFromDiskExporter);
    }

    @VisibleForTesting
    OpenTelemetryRumBuilder setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        return this;
    }

    /**
     * Sets a scheduler that will take care of periodically read data stored in disk and export it.
     * If not specified, the default schedule exporter will be used.
     */
    public OpenTelemetryRumBuilder setExportScheduleHandler(
            @NonNull ExportScheduleHandler exportScheduleHandler) {
        requireNonNull(exportScheduleHandler, "exportScheduleHandler cannot be null");
        this.exportScheduleHandler = exportScheduleHandler;
        return this;
    }

    private StorageConfiguration createStorageConfiguration(Services services) throws IOException {
        CacheStorage storage = services.getCacheStorage();
        DiskBufferingConfig config = this.config.getDiskBufferingConfig();
        DiskManager diskManager = new DiskManager(storage, config);
        return StorageConfiguration.builder()
                .setRootDir(diskManager.getSignalsBufferDir())
                .setMaxFileSize(diskManager.getMaxCacheFileSize())
                .setMaxFolderSize(diskManager.getMaxFolderSize())
                .setMaxFileAgeForWriteMillis(config.getMaxFileAgeForWriteMillis())
                .setMaxFileAgeForReadMillis(config.getMaxFileAgeForReadMillis())
                .setMinFileAgeForReadMillis(config.getMinFileAgeForReadMillis())
                .setTemporaryFileProvider(
                        new SimpleTemporaryFileProvider(diskManager.getTemporaryDir()))
                .setDebugEnabled(config.getDebugEnabled())
                .build();
    }

    private void scheduleDiskTelemetryReader(
            Services services, @Nullable SignalFromDiskExporter signalExporter) {
        if (exportScheduleHandler == null) {
            // TODO: Is it safe to get the work service yet here? If so, we can
            // avoid all this lazy supplier stuff....
            Function0<PeriodicWork> getWorkService = services::getPeriodicWork;
            exportScheduleHandler =
                    new DefaultExportScheduleHandler(
                            new DefaultExportScheduler(getWorkService), getWorkService);
        }

        final ExportScheduleHandler exportScheduleHandler =
                requireNonNull(this.exportScheduleHandler);

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
    private void applyConfiguration(Services services, InitializationEvents initializationEvents) {
        if (config.shouldGenerateSdkInitializationEvents()) {
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
            addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) -> {
                        SpanProcessor networkAttributesSpanAppender =
                                NetworkAttributesSpanAppender.create(
                                        services.getCurrentNetworkProvider());
                        return tracerProviderBuilder.addSpanProcessor(
                                networkAttributesSpanAppender);
                    });
            initializationEvents.currentNetworkProviderInitialized();
        }

        // Add processors that append screen attribute(s)
        if (config.shouldIncludeScreenAttributes()) {
            tracerProviderCustomizers.add(
                    0,
                    (builder, app) ->
                            builder.addSpanProcessor(
                                    new ScreenAttributesSpanProcessor(
                                            services.getVisibleScreenTracker())));
            loggerProviderCustomizers.add(
                    0,
                    (builder, app) ->
                            builder.addLogRecordProcessor(
                                    new ScreenAttributesLogRecordProcessor(
                                            services.getVisibleScreenTracker())));
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
            SessionProvider sessionProvider,
            Application application,
            LogRecordExporter logsExporter) {
        SdkLoggerProviderBuilder loggerProviderBuilder =
                SdkLoggerProvider.builder()
                        .setResource(resource)
                        .addLogRecordProcessor(new SessionIdLogRecordAppender(sessionProvider))
                        .addLogRecordProcessor(
                                new GlobalAttributesLogRecordAppender(
                                        config.getGlobalAttributesSupplier()));
        LogRecordProcessor batchLogsProcessor =
                BatchLogRecordProcessor.builder(logsExporter).build();
        loggerProviderBuilder.addLogRecordProcessor(batchLogsProcessor);
        for (BiFunction<SdkLoggerProviderBuilder, Application, SdkLoggerProviderBuilder>
                customizer : loggerProviderCustomizers) {
            loggerProviderBuilder = customizer.apply(loggerProviderBuilder, application);
        }
        return loggerProviderBuilder.build();
    }

    private SpanExporter buildSpanExporter() {
        // TODO: Default to otlp...but how can we make endpoint and auth mandatory?
        SpanExporter defaultExporter = LoggingSpanExporter.create();
        return spanExporterCustomizer.apply(defaultExporter);
    }

    private MetricExporter buildMetricExporter() {
        // TODO: Default to otlp...but how can we make endpoint and auth mandatory?
        MetricExporter defaultExporter = LoggingMetricExporter.create();
        return metricExporterCustomizer.apply(defaultExporter);
    }

    private LogRecordExporter buildLogsExporter() {
        // TODO: Default to otlp...but how can we make endpoint and auth mandatory?
        LogRecordExporter defaultExporter = SystemOutLogRecordExporter.create();
        return logRecordExporterCustomizer.apply(defaultExporter);
    }

    private SdkMeterProvider buildMeterProvider(
            Application application, MetricExporter metricExporter) {
        MetricReader reader = PeriodicMetricReader.create(metricExporter);
        SdkMeterProviderBuilder meterProviderBuilder =
                SdkMeterProvider.builder().registerMetricReader(reader).setResource(resource);
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
