/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("DEPRECATION") // required to suppress reference of AsyncTask thread pool

package io.opentelemetry.android

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import io.opentelemetry.android.AndroidResource.createDefault
import io.opentelemetry.android.common.RumConstants
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.export.BufferDelegatingLogExporter
import io.opentelemetry.android.export.BufferDelegatingMetricExporter
import io.opentelemetry.android.export.BufferDelegatingSpanExporter
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter.Companion.set
import io.opentelemetry.android.features.diskbuffering.scheduler.DefaultExportScheduleHandler
import io.opentelemetry.android.features.diskbuffering.scheduler.DefaultExportScheduler
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.internal.features.networkattrs.NetworkAttributesLogRecordAppender
import io.opentelemetry.android.internal.features.networkattrs.NetworkAttributesSpanAppender.Companion.create
import io.opentelemetry.android.internal.features.persistence.DiskManager
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.android.internal.processors.GlobalAttributesLogRecordAppender
import io.opentelemetry.android.internal.processors.ScreenAttributesLogRecordProcessor
import io.opentelemetry.android.internal.processors.SessionIdLogRecordAppender
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.context.propagation.TextMapPropagator.composite
import io.opentelemetry.contrib.disk.buffering.exporters.LogRecordToDiskExporter
import io.opentelemetry.contrib.disk.buffering.exporters.MetricToDiskExporter
import io.opentelemetry.contrib.disk.buffering.exporters.SpanToDiskExporter
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileLogRecordStorage
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileMetricStorage
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileSpanStorage
import io.opentelemetry.contrib.disk.buffering.storage.impl.FileStorageConfiguration
import io.opentelemetry.exporter.logging.LoggingMetricExporter
import io.opentelemetry.exporter.logging.LoggingSpanExporter
import io.opentelemetry.exporter.logging.SystemOutLogRecordExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.LogRecordProcessor
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.MetricReader
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import java.io.File
import java.io.IOException
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function

/**
 * A builder of [OpenTelemetryRum]. It enabled configuring the OpenTelemetry SDK and disabling
 * built-in Android instrumentations.
 *
 *
 * This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
class OpenTelemetryRumBuilder internal constructor(
    private val context: Context,
    private val config: OtelRumConfig,
) {
    companion object {
        /**
         * Creates a new [OpenTelemetryRumBuilder] instance using the values from the supplied
         * [OtelRumConfig].
         */
        fun create(
            context: Context,
            config: OtelRumConfig,
        ): OpenTelemetryRumBuilder = OpenTelemetryRumBuilder(context, config)
    }

    private val tracerProviderCustomizers: MutableList<BiFunction<SdkTracerProviderBuilder, Context, SdkTracerProviderBuilder>> =
        mutableListOf()
    private val meterProviderCustomizers: MutableList<BiFunction<SdkMeterProviderBuilder, Context, SdkMeterProviderBuilder>> =
        mutableListOf()
    private val loggerProviderCustomizers: MutableList<BiFunction<SdkLoggerProviderBuilder, Context, SdkLoggerProviderBuilder>> =
        mutableListOf()
    private val instrumentations: MutableList<AndroidInstrumentation> =
        mutableListOf()
    private val otelSdkReadyListeners: MutableList<Consumer<OpenTelemetrySdk>> =
        mutableListOf()

    private var spanExporterCustomizer: (SpanExporter) -> SpanExporter = { it }
    private var metricExporterCustomizer: (MetricExporter) -> MetricExporter = { it }
    private var logRecordExporterCustomizer: (LogRecordExporter) -> LogRecordExporter = { it }
    private var propagatorCustomizer: (TextMapPropagator) -> TextMapPropagator = { it }

    private var resource: Resource = createDefault(context)
    private var exportScheduleHandler: ExportScheduleHandler? = null
    private var sessionProvider: SessionProvider = SessionProvider.getNoop()

    /**
     * Assign a [Resource] to be attached to all telemetry emitted by the [OpenTelemetryRum]
     * created by this builder. This replaces any existing resource.
     */
    fun setResource(resource: Resource): OpenTelemetryRumBuilder {
        this.resource = resource
        return this
    }

    /**
     * Merges a new [Resource] with any existing [Resource] in this builder. The
     * resulting [Resource] will be attached to all telemetry emitted by the [OpenTelemetryRum]
     * created by this builder.
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
     * via [.setResource].
     */
    fun addTracerProviderCustomizer(
        customizer: BiFunction<SdkTracerProviderBuilder, Context, SdkTracerProviderBuilder>,
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
     * via [.setResource].
     */
    fun addMeterProviderCustomizer(
        customizer: BiFunction<SdkMeterProviderBuilder, Context, SdkMeterProviderBuilder>,
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
     * via [.setResource].
     */
    fun addLoggerProviderCustomizer(
        customizer: BiFunction<SdkLoggerProviderBuilder, Context, SdkLoggerProviderBuilder>,
    ): OpenTelemetryRumBuilder {
        loggerProviderCustomizers.add(customizer)
        return this
    }

    /**
     * Adds an instrumentation to be applied as a part of the [.build] method call.
     */
    fun addInstrumentation(instrumentation: AndroidInstrumentation): OpenTelemetryRumBuilder {
        instrumentations.add(instrumentation)
        return this
    }

    /**
     * Adds a [Function] to invoke with the default [TextMapPropagator] to allow
     * customization. The return value of the [BiFunction] will replace the passed-in
     * argument. To add new propagators, use `TextMapPropagator.composite()` with the existing
     * propagator passed to your function.
     *
     * Multiple calls will execute the customizers in order.
     */
    fun addPropagatorCustomizer(propagatorCustomizer: Function<in TextMapPropagator, out TextMapPropagator>): OpenTelemetryRumBuilder {
        val existing = this.propagatorCustomizer
        this.propagatorCustomizer = {
            propagatorCustomizer.apply(existing(it))
        }
        return this
    }

    /**
     * Adds a [Function] to invoke with the default [SpanExporter] to allow
     * customization. The return value of the [Function] will replace the passed-in argument.
     *
     * Multiple calls will execute the customizers in order.
     */
    fun addSpanExporterCustomizer(spanExporterCustomizer: Function<in SpanExporter, out SpanExporter>): OpenTelemetryRumBuilder {
        val existing = this.spanExporterCustomizer
        this.spanExporterCustomizer = {
            spanExporterCustomizer.apply(existing(it))
        }
        return this
    }

    /**
     * Adds a [Function] to invoke with the default [MetricExporter] to allow
     * customization. The return value of the [Function] will replace the passed-in argument.
     *
     * Multiple calls will execute the customizers in order.
     */
    fun addMetricExporterCustomizer(metricExporterCustomizer: Function<in MetricExporter, out MetricExporter>): OpenTelemetryRumBuilder {
        val existing = this.metricExporterCustomizer
        this.metricExporterCustomizer = {
            metricExporterCustomizer.apply(existing(it))
        }
        return this
    }

    /**
     * Adds a [Function] to invoke with the default [LogRecordExporter] to allow
     * customization. The return value of the [Function] will replace the passed-in argument.
     *
     * Multiple calls will execute the customizers in order.
     */
    fun addLogRecordExporterCustomizer(
        logRecordExporterCustomizer: Function<in LogRecordExporter, out LogRecordExporter>,
    ): OpenTelemetryRumBuilder {
        val existing = this.logRecordExporterCustomizer
        this.logRecordExporterCustomizer = {
            logRecordExporterCustomizer.apply(existing(it))
        }
        return this
    }

    /**
     * Sets a custom [SessionProvider] that controls the session ID.
     */
    fun setSessionProvider(sessionProvider: SessionProvider): OpenTelemetryRumBuilder {
        this.sessionProvider = sessionProvider
        return this
    }

    /**
     * Sets a scheduler that will take care of periodically read data stored in disk and export it.
     * If not specified, the default schedule exporter will be used.
     */
    fun setExportScheduleHandler(exportScheduleHandler: ExportScheduleHandler): OpenTelemetryRumBuilder {
        this.exportScheduleHandler = exportScheduleHandler
        return this
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

    /**
     * Creates a new instance of [OpenTelemetryRum] with the settings of this [OpenTelemetryRum].
     *
     * This method will initialize the OpenTelemetry SDK and install built-in system
     * instrumentations in the passed Android [Context].
     *
     * @return A new [OpenTelemetryRum] instance.
     */
    fun build(): OpenTelemetryRum {
        val services = Services.get(context)
        val initializationEvents = InitializationEvents.get()
        applyConfiguration(services, initializationEvents)

        val bufferDelegatingSpanExporter = BufferDelegatingSpanExporter()
        val bufferDelegatingLogExporter = BufferDelegatingLogExporter()
        val bufferDelegatingMetricExporter = BufferDelegatingMetricExporter()

        val sdk =
            OpenTelemetrySdk
                .builder()
                .setTracerProvider(
                    buildTracerProvider(
                        sessionProvider,
                        context,
                        bufferDelegatingSpanExporter,
                    ),
                ).setLoggerProvider(
                    buildLoggerProvider(
                        sessionProvider,
                        context,
                        bufferDelegatingLogExporter,
                    ),
                ).setMeterProvider(
                    buildMeterProvider(context, bufferDelegatingMetricExporter),
                ).setPropagators(buildFinalPropagators())
                .build()

        otelSdkReadyListeners.forEach(
            Consumer {
                it.accept(sdk)
            },
        )

        val delegate =
            SdkPreconfiguredRumBuilder(context, sdk, sessionProvider, config)
                .setShutdownHook {
                    exportScheduleHandler?.disable()
                    services.close()
                }

        // AsyncTask is deprecated but the thread pool is still used all over the Android SDK
        // and it provides a way to get a background thread without having to create a new one.
        AsyncTask.THREAD_POOL_EXECUTOR.execute {
            initializeExporters(
                services,
                initializationEvents,
                bufferDelegatingSpanExporter,
                bufferDelegatingLogExporter,
                bufferDelegatingMetricExporter,
            )
        }
        instrumentations.forEach(Consumer(delegate::addInstrumentation))
        return delegate.build()
    }

    private fun initializeExporters(
        services: Services,
        initializationEvents: InitializationEvents,
        bufferDelegatingSpanExporter: BufferDelegatingSpanExporter,
        bufferedDelegatingLogExporter: BufferDelegatingLogExporter,
        bufferDelegatingMetricExporter: BufferDelegatingMetricExporter,
    ) {
        val diskBufferingConfig = config.getDiskBufferingConfig()
        var spanExporter = buildSpanExporter()
        var logsExporter = buildLogsExporter()
        var metricExporter = buildMetricExporter()
        var signalFromDiskExporter: SignalFromDiskExporter? = null

        if (diskBufferingConfig.enabled) {
            try {
                val storage = services.cacheStorage
                val diskManager = DiskManager(storage, diskBufferingConfig)

                val signalsRoot = diskManager.signalsBufferDir
                val spansDir = File(signalsRoot, "spans")
                val metricsDir = File(signalsRoot, "metrics")
                val logsDir = File(signalsRoot, "logs")
                val fileConfig = createStorageConfiguration(diskManager)
                val spanStorage = FileSpanStorage.create(spansDir, fileConfig)
                val logStorage = FileLogRecordStorage.create(logsDir, fileConfig)
                val metricStorage = FileMetricStorage.create(metricsDir, fileConfig)

                val originalSpanExporter = spanExporter
                spanExporter = SpanToDiskExporter.builder(spanStorage).build()
                val originalLogsExporter = logsExporter
                logsExporter = LogRecordToDiskExporter.builder(logStorage).build()
                val originalMetricExporter = metricExporter
                metricExporter = MetricToDiskExporter.builder(metricStorage).build()
                signalFromDiskExporter =
                    SignalFromDiskExporter(
                        spanStorage,
                        originalSpanExporter,
                        logStorage,
                        originalLogsExporter,
                        metricStorage,
                        originalMetricExporter,
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

    @Throws(IOException::class)
    private fun createStorageConfiguration(diskManager: DiskManager): FileStorageConfiguration {
        val config = this.config.getDiskBufferingConfig()
        return FileStorageConfiguration
            .builder()
            .setMaxFileSize(diskManager.maxCacheFileSize)
            .setMaxFolderSize(diskManager.maxFolderSize)
            .setMaxFileAgeForWriteMillis(config.maxFileAgeForWriteMillis)
            .setMaxFileAgeForReadMillis(config.maxFileAgeForReadMillis)
            .setMinFileAgeForReadMillis(config.minFileAgeForReadMillis)
            .build()
    }

    private fun scheduleDiskTelemetryReader(
        services: Services,
        signalExporter: SignalFromDiskExporter?,
    ) {
        // TODO: Is it safe to get the work service yet here? If so, we can
        // avoid all this lazy supplier stuff....
        val handler =
            exportScheduleHandler ?: DefaultExportScheduleHandler(
                DefaultExportScheduler(services::periodicWork),
                services::periodicWork,
            )

        if (signalExporter == null) {
            // Disabling here allows to cancel previously scheduled exports using tools that
            // can run even after the app has been terminated (such as WorkManager).
            // But for in-memory only schedulers, nothing should need to be disabled.
            handler.disable()
        } else {
            // Not null means that disk buffering is enabled and disk exporters are successfully
            // initialized.
            set(signalExporter)
            handler.enable()
        }
    }

    /** Leverage the configuration to wire up various instrumentation components.  */
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
            val appender = GlobalAttributesSpanAppender(config.getGlobalAttributesSupplier())
            addTracerProviderCustomizer { tracerProviderBuilder: SdkTracerProviderBuilder, _: Context ->
                tracerProviderBuilder.addSpanProcessor(appender)
            }
        }

        // Network specific attributes
        if (config.shouldIncludeNetworkAttributes()) {
            val networkProvider = services.currentNetworkProvider
            // Add span processor that appends network attributes.
            addTracerProviderCustomizer { tracerProviderBuilder: SdkTracerProviderBuilder, _: Context ->
                val networkAttributesSpanAppender = create(networkProvider)
                tracerProviderBuilder.addSpanProcessor(networkAttributesSpanAppender)
            }
            // Add log record processor that appends network attributes.
            addLoggerProviderCustomizer { builder: SdkLoggerProviderBuilder, _: Context ->
                val processor = NetworkAttributesLogRecordAppender(networkProvider)
                builder.addLogRecordProcessor(processor)
            }
            initializationEvents.currentNetworkProviderInitialized()
        }

        // Add processors that append screen attribute(s)
        if (config.shouldIncludeScreenAttributes()) {
            tracerProviderCustomizers.add(
                0,
                BiFunction { builder: SdkTracerProviderBuilder, _: Context ->
                    builder.addSpanProcessor(
                        ScreenAttributesSpanProcessor(
                            services.visibleScreenTracker,
                        ),
                    )
                },
            )
            loggerProviderCustomizers.add(
                0,
                BiFunction { builder: SdkLoggerProviderBuilder, _: Context ->
                    builder.addLogRecordProcessor(
                        ScreenAttributesLogRecordProcessor(
                            services.visibleScreenTracker,
                        ),
                    )
                },
            )
        }
    }

    private fun buildTracerProvider(
        sessionProvider: SessionProvider,
        context: Context,
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
            tracerProviderBuilder = customizer.apply(tracerProviderBuilder, context)
        }
        return tracerProviderBuilder.build()
    }

    private fun buildLoggerProvider(
        sessionProvider: SessionProvider,
        context: Context,
        logsExporter: LogRecordExporter,
    ): SdkLoggerProvider {
        var loggerProviderBuilder =
            SdkLoggerProvider
                .builder()
                .setResource(resource)
                .addLogRecordProcessor(SessionIdLogRecordAppender(sessionProvider))
                .addLogRecordProcessor(
                    GlobalAttributesLogRecordAppender(
                        config.getGlobalAttributesSupplier(),
                    ),
                )
        val batchLogsProcessor: LogRecordProcessor =
            BatchLogRecordProcessor.builder(logsExporter).build()
        loggerProviderBuilder.addLogRecordProcessor(batchLogsProcessor)
        for (customizer in loggerProviderCustomizers) {
            loggerProviderBuilder = customizer.apply(loggerProviderBuilder, context)
        }
        return loggerProviderBuilder.build()
    }

    private fun buildSpanExporter(): SpanExporter {
        val defaultExporter: SpanExporter = LoggingSpanExporter.create()
        return spanExporterCustomizer(defaultExporter)
    }

    private fun buildMetricExporter(): MetricExporter {
        val defaultExporter: MetricExporter = LoggingMetricExporter.create()
        return metricExporterCustomizer(defaultExporter)
    }

    private fun buildLogsExporter(): LogRecordExporter {
        val defaultExporter: LogRecordExporter = SystemOutLogRecordExporter.create()
        return logRecordExporterCustomizer(defaultExporter)
    }

    private fun buildMeterProvider(
        context: Context,
        metricExporter: MetricExporter,
    ): SdkMeterProvider {
        val reader: MetricReader = PeriodicMetricReader.create(metricExporter)
        var meterProviderBuilder =
            SdkMeterProvider.builder().registerMetricReader(reader).setResource(resource)
        for (customizer in meterProviderCustomizers) {
            meterProviderBuilder = customizer.apply(meterProviderBuilder, context)
        }
        return meterProviderBuilder.build()
    }

    private fun buildFinalPropagators(): ContextPropagators {
        val defaultPropagator: TextMapPropagator =
            composite(
                W3CTraceContextPropagator.getInstance(),
                W3CBaggagePropagator.getInstance(),
            )
        return ContextPropagators.create(propagatorCustomizer(defaultPropagator))
    }
}
