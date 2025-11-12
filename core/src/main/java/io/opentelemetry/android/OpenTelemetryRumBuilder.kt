/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import android.util.Log
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.export.BufferDelegatingLogExporter
import io.opentelemetry.android.export.BufferDelegatingMetricExporter
import io.opentelemetry.android.export.BufferDelegatingSpanExporter
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.features.diskbuffering.scheduler.DefaultExportScheduleHandler
import io.opentelemetry.android.features.diskbuffering.scheduler.DefaultExportScheduler
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.internal.features.networkattrs.NetworkAttributesLogRecordAppender
import io.opentelemetry.android.internal.features.networkattrs.NetworkAttributesSpanAppender
import io.opentelemetry.android.internal.features.persistence.DiskManager
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.android.internal.processors.GlobalAttributesLogRecordAppender
import io.opentelemetry.android.internal.processors.ScreenAttributesLogRecordProcessor
import io.opentelemetry.android.internal.processors.SessionIdLogRecordAppender
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.periodicwork.PeriodicWork
import io.opentelemetry.android.internal.services.storage.CacheStorage
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapPropagator.composite
import io.opentelemetry.contrib.disk.buffering.LogRecordFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.LogRecordToDiskExporter
import io.opentelemetry.contrib.disk.buffering.MetricFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.MetricToDiskExporter
import io.opentelemetry.contrib.disk.buffering.SpanFromDiskExporter
import io.opentelemetry.contrib.disk.buffering.SpanToDiskExporter
import io.opentelemetry.contrib.disk.buffering.config.StorageConfiguration
import io.opentelemetry.contrib.disk.buffering.internal.storage.Storage
import io.opentelemetry.contrib.disk.buffering.internal.utils.SignalTypes
import io.opentelemetry.exporter.logging.LoggingMetricExporter
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.io.IOException
import java.util.function.BiFunction
import java.util.function.Consumer

/**
 * A builder of [OpenTelemetryRum]. It enabled configuring the OpenTelemetry SDK and disabling
 * built-in Android instrumentations.
 *
 * This class is part of the core module and is not a supported public API. Its APIs can change
 * or be removed at any time without a major version change.
 */
class OpenTelemetryRumBuilder(
    private val application: Application,
    private val config: OtelRumConfig,
) {
    private val tracerProviderCustomizers: MutableList<BiFunction<SdkTracerProviderBuilder, Application, SdkTracerProviderBuilder>> =
        mutableListOf()
    private val meterProviderCustomizers: MutableList<BiFunction<SdkMeterProviderBuilder, Application, SdkMeterProviderBuilder>> =
        mutableListOf()
    private val loggerProviderCustomizers: MutableList<BiFunction<SdkLoggerProviderBuilder, Application, SdkLoggerProviderBuilder>> =
        mutableListOf()
    private val instrumentations: MutableList<AndroidInstrumentation> = mutableListOf()
    private val otelSdkReadyListeners: MutableList<Consumer<OpenTelemetrySdk>> = mutableListOf()

    private var spanExporterCustomizer: (SpanExporter) -> SpanExporter = { it }
    private var metricExporterCustomizer: (MetricExporter) -> MetricExporter = { it }
    private var logRecordExporterCustomizer: (LogRecordExporter) -> LogRecordExporter = { it }
    private var propagatorCustomizer: (TextMapPropagator) -> TextMapPropagator = { it }

    private var resource: Resource = AndroidResource.createDefault(application)
    private var exportScheduleHandler: ExportScheduleHandler? = null
    private var sessionProvider: SessionProvider? = null

    /**
     * Assign a [Resource] to be attached to all telemetry emitted by the [OpenTelemetryRum]
     * created by this builder. This replaces any existing resource.
     *
     * @return `this`
     */
    fun setResource(resource: Resource): OpenTelemetryRumBuilder {
        this.resource = resource
        return this
    }

    /**
     * Merges a new [Resource] with any existing [Resource] in this builder. The
     * resulting [Resource] will be attached to all telemetry emitted by the [OpenTelemetryRum]
     * created by this builder.
     *
     * @return `this`
     */
    fun mergeResource(resource: Resource): OpenTelemetryRumBuilder {
        this.resource = this.resource.merge(resource)
        return this
    }

    /**
     * Adds a [BiFunction] to invoke the with the [SdkTracerProviderBuilder] to allow
     * customization. The return value of the [BiFunction] will replace the passed-in
     * argument.
     *
     * Multiple calls will execute the customizers in order.
     *
     * Note: calling [SdkTracerProviderBuilder.setResource] inside of your
     * configuration function will cause any resource customizers to be ignored that were configured
     * via [setResource].
     *
     * @return `this`
     */
    fun addTracerProviderCustomizer(
        customizer: BiFunction<SdkTracerProviderBuilder, Application, SdkTracerProviderBuilder>,
    ): OpenTelemetryRumBuilder {
        tracerProviderCustomizers.add(customizer)
        return this
    }

    /**
     * Adds a [BiFunction] to invoke the with the [SdkMeterProviderBuilder] to allow
     * customization. The return value of the [BiFunction] will replace the passed-in
     * argument.
     *
     * Multiple calls will execute the customizers in order.
     *
     * Note: calling [SdkMeterProviderBuilder.setResource] inside of your
     * configuration function will cause any resource customizers to be ignored that were configured
     * via [setResource].
     *
     * @return `this`
     */
    fun addMeterProviderCustomizer(
        customizer: BiFunction<SdkMeterProviderBuilder, Application, SdkMeterProviderBuilder>,
    ): OpenTelemetryRumBuilder {
        meterProviderCustomizers.add(customizer)
        return this
    }

    /**
     * Adds a [BiFunction] to invoke the with the [SdkLoggerProviderBuilder] to allow
     * customization. The return value of the [BiFunction] will replace the passed-in
     * argument.
     *
     * Multiple calls will execute the customizers in order.
     *
     * Note: calling [SdkLoggerProviderBuilder.setResource] inside of your
     * configuration function will cause any resource customizers to be ignored that were configured
     * via [setResource].
     *
     * @return `this`
     */
    fun addLoggerProviderCustomizer(
        customizer: BiFunction<SdkLoggerProviderBuilder, Application, SdkLoggerProviderBuilder>,
    ): OpenTelemetryRumBuilder {
        loggerProviderCustomizers.add(customizer)
        return this
    }

    /**
     * Adds an instrumentation to be applied as a part of the [build] method call.
     *
     * @return `this`
     */
    fun addInstrumentation(instrumentation: AndroidInstrumentation): OpenTelemetryRumBuilder {
        instrumentations.add(instrumentation)
        return this
    }

    /**
     * Adds a function to invoke with the default [TextMapPropagator] to allow
     * customization. The return value of the [BiFunction] will replace the passed-in
     * argument. To add new propagators, use `TextMapPropagator.composite()` with the existing
     * propagator passed to your function.
     *
     * Multiple calls will execute the customizers in order.
     */
    fun addPropagatorCustomizer(newCustomizer: (TextMapPropagator) -> TextMapPropagator): OpenTelemetryRumBuilder {
        val existing = propagatorCustomizer
        propagatorCustomizer = { propagator ->
            val result = existing(propagator)
            newCustomizer(result)
        }
        return this
    }

    /**
     * Adds a function to invoke with the default [SpanExporter] to allow
     * customization. The return value of the function will replace the passed-in argument.
     *
     * Multiple calls will execute the customizers in order.
     */
    fun addSpanExporterCustomizer(newCustomizer: (SpanExporter) -> SpanExporter): OpenTelemetryRumBuilder {
        val existing = spanExporterCustomizer
        spanExporterCustomizer = { exporter ->
            val intermediate = existing(exporter)
            newCustomizer(intermediate)
        }
        return this
    }

    /**
     * Adds a function to invoke with the default [MetricExporter] to allow
     * customization. The return value of the function will replace the passed-in argument.
     *
     * Multiple calls will execute the customizers in order.
     */
    fun addMetricExporterCustomizer(newCustomizer: (MetricExporter) -> MetricExporter): OpenTelemetryRumBuilder {
        val existing = metricExporterCustomizer
        metricExporterCustomizer = { exporter ->
            val intermediate = existing(exporter)
            newCustomizer(intermediate)
        }
        return this
    }

    /**
     * Adds a function to invoke with the default [LogRecordExporter] to allow
     * customization. The return value of the function will replace the passed-in argument.
     *
     * Multiple calls will execute the customizers in order.
     */
    fun addLogRecordExporterCustomizer(newCustomizer: (LogRecordExporter) -> LogRecordExporter): OpenTelemetryRumBuilder {
        val existing = logRecordExporterCustomizer
        logRecordExporterCustomizer = { exporter ->
            val intermediate = existing(exporter)
            newCustomizer(intermediate)
        }
        return this
    }

    /**
     * Creates a new instance of [OpenTelemetryRum] with the settings of this
     * [OpenTelemetryRumBuilder].
     *
     * This method will initialize the OpenTelemetry SDK and install built-in system
     * instrumentations in the passed Android [Application].
     *
     * @return A new [OpenTelemetryRum] instance.
     */
    fun build(): OpenTelemetryRum {
        val services = Services.get(application)
        val initializationEvents = InitializationEvents.get()
        applyConfiguration(services, initializationEvents)
        if (sessionProvider == null) {
            sessionProvider = SessionProvider.getNoop()
        }

        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter()
        val bufferDelegatingLogExporter = BufferDelegatingLogExporter()
        val bufferDelegatingMetricExporter = BufferDelegatingMetricExporter()

        val sdk =
            OpenTelemetrySdk
                .builder()
                .setTracerProvider(
                    buildTracerProvider(
                        sessionProvider!!,
                        application,
                        bufferDelegatingSpanExporter,
                    ),
                ).setLoggerProvider(
                    buildLoggerProvider(
                        sessionProvider!!,
                        application,
                        bufferDelegatingLogExporter,
                    ),
                ).setMeterProvider(
                    buildMeterProvider(application, bufferDelegatingMetricExporter),
                ).setPropagators(buildFinalPropagators())
                .build()

        otelSdkReadyListeners.forEach { listener -> listener.accept(sdk) }

        val delegate =
            SdkPreconfiguredRumBuilder(application, sdk, sessionProvider!!, config)
                .setShutdownHook {
                    exportScheduleHandler?.disable()
                    services.close()
                }

        // AsyncTask is deprecated but the thread pool is still used all over the Android SDK
        // and it provides a way to get a background thread without having to create a new one.
        android.os.AsyncTask.THREAD_POOL_EXECUTOR.execute {
            initializeExporters(
                services,
                initializationEvents,
                bufferDelegatingSpanExporter,
                bufferDelegatingLogExporter,
                bufferDelegatingMetricExporter,
            )
        }

        instrumentations.forEach { delegate.addInstrumentation(it) }

        return delegate.build()
    }

    private fun initializeExporters(
        services: Services,
        initializationEvents: InitializationEvents,
        bufferDelegatingSpanExporter: BufferDelegatingSpanExporter,
        bufferedDelegatingLogExporter: BufferDelegatingLogExporter,
        bufferDelegatingMetricExporter: BufferDelegatingMetricExporter,
    ) {
        val diskBufferingConfig = config.diskBufferingConfig
        var spanExporter = buildSpanExporter()
        var logsExporter = buildLogsExporter()
        var metricExporter = buildMetricExporter()
        var signalFromDiskExporter: SignalFromDiskExporter? = null

        if (diskBufferingConfig.enabled) {
            try {
                val storageConfiguration = createStorageConfiguration(services)
                val spanStorage =
                    Storage
                        .builder(SignalTypes.spans)
                        .setStorageConfiguration(storageConfiguration)
                        .build()
                val logsStorage =
                    Storage
                        .builder(SignalTypes.logs)
                        .setStorageConfiguration(storageConfiguration)
                        .build()
                val metricsStorage =
                    Storage
                        .builder(SignalTypes.metrics)
                        .setStorageConfiguration(storageConfiguration)
                        .build()

                val originalSpanExporter = spanExporter
                spanExporter = SpanToDiskExporter.create(originalSpanExporter, spanStorage)
                val originalLogsExporter = logsExporter
                logsExporter = LogRecordToDiskExporter.create(originalLogsExporter, logsStorage)
                val originalMetricExporter = metricExporter
                metricExporter =
                    MetricToDiskExporter.create(originalMetricExporter, metricsStorage)
                signalFromDiskExporter =
                    SignalFromDiskExporter(
                        SpanFromDiskExporter.create(originalSpanExporter, spanStorage),
                        MetricFromDiskExporter.create(
                            originalMetricExporter,
                            metricsStorage,
                        ),
                        LogRecordFromDiskExporter.create(
                            originalLogsExporter,
                            logsStorage,
                        ),
                    )
            } catch (e: IOException) {
                Log.e(RumConstants.OTEL_RUM_LOG_TAG, "Could not initialize disk exporters.", e)
            }
        }
        initializationEvents.spanExporterInitialized(spanExporter)
        bufferedDelegatingLogExporter.setDelegate(logsExporter)
        bufferDelegatingSpanExporter.setDelegate(spanExporter)
        bufferDelegatingMetricExporter.setDelegate(metricExporter)
        scheduleDiskTelemetryReader(services, signalFromDiskExporter)
    }

    fun setSessionProvider(provider: SessionProvider): OpenTelemetryRumBuilder {
        sessionProvider = provider
        return this
    }

    /**
     * Sets a scheduler that will take care of periodically read data stored in disk and export it.
     * If not specified, the default schedule exporter will be used.
     */
    fun setExportScheduleHandler(handler: ExportScheduleHandler): OpenTelemetryRumBuilder {
        exportScheduleHandler = handler
        return this
    }

    private fun createStorageConfiguration(services: Services): StorageConfiguration {
        val storage: CacheStorage = services.cacheStorage
        val config = config.diskBufferingConfig
        val diskManager = DiskManager(storage, config)
        return StorageConfiguration
            .builder()
            .setRootDir(diskManager.signalsBufferDir)
            .setMaxFileSize(diskManager.maxCacheFileSize)
            .setMaxFolderSize(diskManager.maxFolderSize)
            .setMaxFileAgeForWriteMillis(config.maxFileAgeForWriteMillis)
            .setMaxFileAgeForReadMillis(config.maxFileAgeForReadMillis)
            .setMinFileAgeForReadMillis(config.minFileAgeForReadMillis)
            .setDebugEnabled(config.debugEnabled)
            .build()
    }

    private fun scheduleDiskTelemetryReader(
        services: Services,
        signalExporter: SignalFromDiskExporter?,
    ) {
        if (exportScheduleHandler == null) {
            // TODO: Is it safe to get the work service yet here? If so, we can
            // avoid all this lazy supplier stuff....
            val getWorkService: () -> PeriodicWork = { services.periodicWork }
            exportScheduleHandler =
                DefaultExportScheduleHandler(
                    DefaultExportScheduler(getWorkService),
                    getWorkService,
                )
        }

        val exportScheduleHandler = requireNotNull(this.exportScheduleHandler)

        if (signalExporter == null) {
            // Disabling here allows to cancel previously scheduled exports using tools that
            // can run even after the app has been terminated (such as WorkManager).
            // But for in-memory only schedulers, nothing should need to be disabled.
            exportScheduleHandler.disable()
        } else {
            // Not null means that disk buffering is enabled and disk exporters are successfully
            // initialized.
            SignalFromDiskExporter.set(signalExporter)
            exportScheduleHandler.enable()
        }
    }

    /**
     * Adds a callback to be invoked after the OpenTelemetry SDK has been initialized. This can be
     * used to defer some early lifecycle functionality until the working SDK is ready.
     *
     * @param callback - A callback that receives the OpenTelemetry SDK instance.
     * @return this
     */
    fun addOtelSdkReadyListener(callback: Consumer<OpenTelemetrySdk>): OpenTelemetryRumBuilder {
        otelSdkReadyListeners.add(callback)
        return this
    }

    /** Leverage the configuration to wire up various instrumentation components. */
    private fun applyConfiguration(
        services: Services,
        initializationEvents: InitializationEvents,
    ) {
        if (config.shouldGenerateSdkInitializationEvents()) {
            initializationEvents.recordConfiguration(config)
        }
        initializationEvents.sdkInitializationStarted()

        // Global attributes
        if (config.hasGlobalAttributes()) {
            // Add span processor that appends global attributes.
            val appender =
                GlobalAttributesSpanAppender(config.globalAttributesSupplier)
            addTracerProviderCustomizer { tracerProviderBuilder, _ ->
                tracerProviderBuilder.addSpanProcessor(appender)
            }
        }

        // Network specific attributes
        if (config.shouldIncludeNetworkAttributes()) {
            val networkProvider = services.currentNetworkProvider
            // Add span processor that appends network attributes.
            addTracerProviderCustomizer { tracerProviderBuilder, _ ->
                val networkAttributesSpanAppender =
                    NetworkAttributesSpanAppender.create(networkProvider)
                tracerProviderBuilder.addSpanProcessor(
                    networkAttributesSpanAppender,
                )
            }
            // Add log record processor that appends network attributes.
            addLoggerProviderCustomizer { builder, _ ->
                val processor =
                    NetworkAttributesLogRecordAppender(networkProvider)
                builder.addLogRecordProcessor(processor)
            }
            initializationEvents.currentNetworkProviderInitialized()
        }

        // Add processors that append screen attribute(s)
        if (config.shouldIncludeScreenAttributes()) {
            tracerProviderCustomizers.add(0) { builder, _ ->
                builder.addSpanProcessor(
                    ScreenAttributesSpanProcessor(
                        services.visibleScreenTracker,
                    ),
                )
            }
            loggerProviderCustomizers.add(0) { builder, _ ->
                builder.addLogRecordProcessor(
                    ScreenAttributesLogRecordProcessor(
                        services.visibleScreenTracker,
                    ),
                )
            }
        }
    }

    private fun buildTracerProvider(
        sessionProvider: SessionProvider,
        application: Application,
        spanExporter: SpanExporter,
    ): SdkTracerProvider {
        var tracerProviderBuilder =
            SdkTracerProvider
                .builder()
                .setResource(resource)
                .addSpanProcessor(SessionIdSpanAppender(sessionProvider))

        val batchSpanProcessor = BatchSpanProcessor.builder(spanExporter).build()
        tracerProviderBuilder.addSpanProcessor(batchSpanProcessor)

        for (customizer in tracerProviderCustomizers) {
            tracerProviderBuilder = customizer.apply(tracerProviderBuilder, application)
        }
        return tracerProviderBuilder.build()
    }

    private fun buildLoggerProvider(
        sessionProvider: SessionProvider,
        application: Application,
        logsExporter: LogRecordExporter,
    ): SdkLoggerProvider {
        var loggerProviderBuilder =
            SdkLoggerProvider
                .builder()
                .setResource(resource)
                .addLogRecordProcessor(SessionIdLogRecordAppender(sessionProvider))
                .addLogRecordProcessor(
                    GlobalAttributesLogRecordAppender(
                        config.globalAttributesSupplier,
                    ),
                )
        val batchLogsProcessor = BatchLogRecordProcessor.builder(logsExporter).build()
        loggerProviderBuilder.addLogRecordProcessor(batchLogsProcessor)
        for (customizer in loggerProviderCustomizers) {
            loggerProviderBuilder = customizer.apply(loggerProviderBuilder, application)
        }
        return loggerProviderBuilder.build()
    }

    private fun buildSpanExporter(): SpanExporter = spanExporterCustomizer(LoggingSpanExporter.create())

    private fun buildMetricExporter(): MetricExporter = metricExporterCustomizer(LoggingMetricExporter.create())

    private fun buildLogsExporter(): LogRecordExporter = logRecordExporterCustomizer(SystemOutLogRecordExporter.create())

    private fun buildMeterProvider(
        application: Application,
        metricExporter: MetricExporter,
    ): SdkMeterProvider {
        val reader = PeriodicMetricReader.create(metricExporter)
        var meterProviderBuilder =
            SdkMeterProvider.builder().registerMetricReader(reader).setResource(resource)
        for (customizer in meterProviderCustomizers) {
            meterProviderBuilder = customizer.apply(meterProviderBuilder, application)
        }
        return meterProviderBuilder.build()
    }

    private fun buildFinalPropagators(): ContextPropagators =
        ContextPropagators.create(
            propagatorCustomizer(
                composite(
                    W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance(),
                ),
            ),
        )

    companion object {
        @JvmStatic
        fun create(
            application: Application,
            config: OtelRumConfig,
        ): OpenTelemetryRumBuilder = OpenTelemetryRumBuilder(application, config)
    }
}
