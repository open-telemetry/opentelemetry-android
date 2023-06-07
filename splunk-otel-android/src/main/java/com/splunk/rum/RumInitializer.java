/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static com.splunk.rum.SplunkRum.APP_NAME_KEY;
import static com.splunk.rum.SplunkRum.COMPONENT_APPSTART;
import static com.splunk.rum.SplunkRum.COMPONENT_ERROR;
import static com.splunk.rum.SplunkRum.COMPONENT_KEY;
import static com.splunk.rum.SplunkRum.COMPONENT_UI;
import static com.splunk.rum.SplunkRum.RUM_TRACER_NAME;
import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
import static io.opentelemetry.rum.internal.RumConstants.APP_START_SPAN_NAME;
import static io.opentelemetry.rum.internal.RumConstants.RUM_SDK_VERSION;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEPLOYMENT_ENVIRONMENT;
import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.splunk.android.rum.R;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.rum.internal.GlobalAttributesSpanAppender;
import io.opentelemetry.rum.internal.OpenTelemetryRum;
import io.opentelemetry.rum.internal.OpenTelemetryRumBuilder;
import io.opentelemetry.rum.internal.SessionIdRatioBasedSampler;
import io.opentelemetry.rum.internal.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.rum.internal.instrumentation.anr.AnrDetector;
import io.opentelemetry.rum.internal.instrumentation.crash.CrashReporter;
import io.opentelemetry.rum.internal.instrumentation.lifecycle.AndroidLifecycleInstrumentation;
import io.opentelemetry.rum.internal.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.rum.internal.instrumentation.network.NetworkAttributesSpanAppender;
import io.opentelemetry.rum.internal.instrumentation.network.NetworkChangeMonitor;
import io.opentelemetry.rum.internal.instrumentation.slowrendering.SlowRenderingDetector;
import io.opentelemetry.rum.internal.instrumentation.startup.AppStartupTimer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

class RumInitializer {

    // we're setting a fairly large length limit to capture long stack traces; ~256 lines,
    // assuming 128 chars per line
    static final int MAX_ATTRIBUTE_LENGTH = 256 * 128;

    private final SplunkRumBuilder builder;
    private final Application application;
    private final AppStartupTimer startupTimer;
    private final InitializationEvents initializationEvents;

    RumInitializer(
            SplunkRumBuilder builder, Application application, AppStartupTimer startupTimer) {
        this.builder = builder;
        this.application = application;
        this.startupTimer = startupTimer;
        this.initializationEvents = new InitializationEvents(startupTimer);
    }

    SplunkRum initialize(
            Function<Application, CurrentNetworkProvider> currentNetworkProviderFactory,
            Looper mainLooper) {
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();

        initializationEvents.begin();
        OpenTelemetryRumBuilder otelRumBuilder = OpenTelemetryRum.builder(application);

        otelRumBuilder.mergeResource(createSplunkResource());
        initializationEvents.emit("resourceInitialized");

        CurrentNetworkProvider currentNetworkProvider =
                currentNetworkProviderFactory.apply(application);
        initializationEvents.emit("connectionUtilInitialized");

        // TODO: How truly important is the order of these span processors? The location of event
        // generation should probably not be altered...

        GlobalAttributesSpanAppender globalAttributesSpanAppender =
                GlobalAttributesSpanAppender.create(builder.globalAttributes);

        // Add span processor that appends global attributes.
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) ->
                        tracerProviderBuilder.addSpanProcessor(globalAttributesSpanAppender));

        // Add span processor that appends network attributes.
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) -> {
                    SpanProcessor networkAttributesSpanAppender =
                            NetworkAttributesSpanAppender.create(currentNetworkProvider);
                    return tracerProviderBuilder.addSpanProcessor(networkAttributesSpanAppender);
                });

        // Add span processor that appends screen attributes and generate init event.
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) -> {
                    ScreenAttributesAppender screenAttributesAppender =
                            new ScreenAttributesAppender(visibleScreenTracker);
                    initializationEvents.emit("attributeAppenderInitialized");
                    return tracerProviderBuilder.addSpanProcessor(screenAttributesAppender);
                });

        // Add batch span processor
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) -> {
                    SpanExporter zipkinExporter = buildFilteringExporter(currentNetworkProvider);
                    initializationEvents.emit("exporterInitialized");

                    BatchSpanProcessor batchSpanProcessor =
                            BatchSpanProcessor.builder(zipkinExporter).build();
                    initializationEvents.emit("batchSpanProcessorInitialized");
                    return tracerProviderBuilder.addSpanProcessor(batchSpanProcessor);
                });

        // Set span limits
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) ->
                        tracerProviderBuilder.setSpanLimits(
                                SpanLimits.builder()
                                        .setMaxAttributeValueLength(MAX_ATTRIBUTE_LENGTH)
                                        .build()));

        // Set up the sampler, if enabled
        if (builder.sessionBasedSamplerEnabled) {
            otelRumBuilder.addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) -> {
                        SessionIdRatioBasedSampler sampler =
                                new SessionIdRatioBasedSampler(
                                        builder.sessionBasedSamplerRatio,
                                        otelRumBuilder.getSessionId());
                        return tracerProviderBuilder.setSampler(sampler);
                    });
        }

        // Wire up the logging exporter, if enabled.
        if (builder.isDebugEnabled()) {
            otelRumBuilder.addTracerProviderCustomizer(
                    (tracerProviderBuilder, app) -> {
                        tracerProviderBuilder.addSpanProcessor(
                                SimpleSpanProcessor.create(
                                        builder.decorateWithSpanFilter(
                                                LoggingSpanExporter.create())));
                        initializationEvents.emit("debugSpanExporterInitialized");
                        return tracerProviderBuilder;
                    });
        }

        // Add final event showing tracer provider init finished
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) -> {
                    initializationEvents.emit("tracerProviderInitialized");
                    return tracerProviderBuilder;
                });

        // install the log->span bridge
        LogToSpanBridge logBridge = new LogToSpanBridge();
        otelRumBuilder.addLoggerProviderCustomizer(
                (loggerProviderBuilder, app) ->
                        loggerProviderBuilder.addLogRecordProcessor(logBridge));
        // make sure the TracerProvider gets set as the very first thing, before any other
        // instrumentations
        otelRumBuilder.addInstrumentation(
                instrumentedApplication ->
                        logBridge.setTracerProvider(
                                instrumentedApplication.getOpenTelemetrySdk().getTracerProvider()));

        if (builder.isAnrDetectionEnabled()) {
            installAnrDetector(otelRumBuilder, mainLooper);
        }
        if (builder.isNetworkMonitorEnabled()) {
            installNetworkMonitor(otelRumBuilder, currentNetworkProvider);
        }
        if (builder.isSlowRenderingDetectionEnabled()) {
            installSlowRenderingDetector(otelRumBuilder);
        }
        if (builder.isCrashReportingEnabled()) {
            installCrashReporter(otelRumBuilder);
        }

        // Lifecycle events instrumentation are always installed.
        installLifecycleInstrumentations(otelRumBuilder, visibleScreenTracker);

        OpenTelemetryRum openTelemetryRum = otelRumBuilder.build();

        initializationEvents.recordInitializationSpans(
                builder.getConfigFlags(),
                openTelemetryRum.getOpenTelemetry().getTracer(RUM_TRACER_NAME));

        return new SplunkRum(openTelemetryRum, globalAttributesSpanAppender);
    }

    private void installLifecycleInstrumentations(
            OpenTelemetryRumBuilder otelRumBuilder, VisibleScreenTracker visibleScreenTracker) {

        otelRumBuilder.addInstrumentation(
                instrumentedApp -> {
                    Function<Tracer, Tracer> tracerCustomizer =
                            tracer ->
                                    (Tracer)
                                            spanName -> {
                                                String component =
                                                        spanName.equals(APP_START_SPAN_NAME)
                                                                ? COMPONENT_APPSTART
                                                                : COMPONENT_UI;
                                                return tracer.spanBuilder(spanName)
                                                        .setAttribute(COMPONENT_KEY, component);
                                            };
                    AndroidLifecycleInstrumentation instrumentation =
                            AndroidLifecycleInstrumentation.builder()
                                    .setVisibleScreenTracker(visibleScreenTracker)
                                    .setStartupTimer(startupTimer)
                                    .setTracerCustomizer(tracerCustomizer)
                                    .setScreenNameExtractor(SplunkScreenNameExtractor.INSTANCE)
                                    .build();
                    instrumentation.installOn(instrumentedApp);
                    initializationEvents.emit("activityLifecycleCallbacksInitialized");
                });
    }

    private Resource createSplunkResource() {
        // applicationName can't be null at this stage
        String applicationName = requireNonNull(builder.applicationName);
        ResourceBuilder resourceBuilder =
                Resource.getDefault().toBuilder().put(APP_NAME_KEY, applicationName);
        if (builder.deploymentEnvironment != null) {
            resourceBuilder.put(DEPLOYMENT_ENVIRONMENT, builder.deploymentEnvironment);
        }
        // TODO: Use the splunk-specific version key and not the upstream one
        return resourceBuilder.put(RUM_SDK_VERSION, detectRumVersion()).build();
    }

    // TODO: Remove this method that is duplicated from upstream AndroidResource
    private String detectRumVersion() {
        try {
            return application
                    .getApplicationContext()
                    .getResources()
                    .getString(R.string.rum_version);
        } catch (Exception e) {
            // ignore for now
        }
        return "unknown";
    }

    private void installAnrDetector(OpenTelemetryRumBuilder otelRumBuilder, Looper mainLooper) {
        otelRumBuilder.addInstrumentation(
                instrumentedApplication -> {
                    AnrDetector.builder()
                            .addAttributesExtractor(constant(COMPONENT_KEY, COMPONENT_ERROR))
                            .setMainLooper(mainLooper)
                            .build()
                            .installOn(instrumentedApplication);

                    initializationEvents.emit("anrMonitorInitialized");
                });
    }

    private void installNetworkMonitor(
            OpenTelemetryRumBuilder otelRumBuilder, CurrentNetworkProvider currentNetworkProvider) {
        otelRumBuilder.addInstrumentation(
                instrumentedApplication -> {
                    NetworkChangeMonitor.create(currentNetworkProvider)
                            .installOn(instrumentedApplication);
                    initializationEvents.emit("networkMonitorInitialized");
                });
    }

    private void installSlowRenderingDetector(OpenTelemetryRumBuilder otelRumBuilder) {
        otelRumBuilder.addInstrumentation(
                instrumentedApplication -> {
                    SlowRenderingDetector.builder()
                            .setSlowRenderingDetectionPollInterval(
                                    builder.slowRenderingDetectionPollInterval)
                            .build()
                            .installOn(instrumentedApplication);
                    initializationEvents.emit("slowRenderingDetectorInitialized");
                });
    }

    private void installCrashReporter(OpenTelemetryRumBuilder otelRumBuilder) {
        otelRumBuilder.addInstrumentation(
                instrumentedApplication -> {
                    CrashReporter.builder()
                            .addAttributesExtractor(
                                    RuntimeDetailsExtractor.create(
                                            instrumentedApplication
                                                    .getApplication()
                                                    .getApplicationContext()))
                            .addAttributesExtractor(new CrashComponentExtractor())
                            .build()
                            .installOn(instrumentedApplication);

                    initializationEvents.emit("crashReportingInitialized");
                });
    }

    // visible for testing
    SpanExporter buildFilteringExporter(CurrentNetworkProvider currentNetworkProvider) {
        SpanExporter exporter = buildExporter(currentNetworkProvider);
        SpanExporter splunkTranslatedExporter =
                new SplunkSpanDataModifier(exporter, builder.isReactNativeSupportEnabled());
        SpanExporter filteredExporter = builder.decorateWithSpanFilter(splunkTranslatedExporter);
        initializationEvents.emit("zipkin exporter initialized");
        return filteredExporter;
    }

    private SpanExporter buildExporter(CurrentNetworkProvider currentNetworkProvider) {
        if (builder.isDebugEnabled()) {
            // tell the Zipkin exporter to shut up already. We're on mobile, network stuff happens.
            // we'll do our best to hang on to the spans with the wrapping BufferingExporter.
            ZipkinSpanExporter.baseLogger.setLevel(Level.SEVERE);
            initializationEvents.emit("logger setup complete");
        }

        if (builder.isDiskBufferingEnabled()) {
            return buildStorageBufferingExporter(currentNetworkProvider);
        }

        return buildMemoryBufferingThrottledExporter(currentNetworkProvider);
    }

    private SpanExporter buildStorageBufferingExporter(
            CurrentNetworkProvider currentNetworkProvider) {
        Sender sender = OkHttpSender.newBuilder().endpoint(getEndpoint()).build();
        File spanFilesPath = FileUtils.getSpansDirectory(application);
        BandwidthTracker bandwidthTracker = new BandwidthTracker();

        FileSender fileSender =
                FileSender.builder().sender(sender).bandwidthTracker(bandwidthTracker).build();
        DiskToZipkinExporter diskToZipkinExporter =
                DiskToZipkinExporter.builder()
                        .connectionUtil(currentNetworkProvider)
                        .fileSender(fileSender)
                        .bandwidthTracker(bandwidthTracker)
                        .spanFilesPath(spanFilesPath)
                        .build();
        diskToZipkinExporter.startPolling();

        return getToDiskExporter();
    }

    @NonNull
    private String getEndpoint() {
        return builder.beaconEndpoint + "?auth=" + builder.rumAccessToken;
    }

    private SpanExporter buildMemoryBufferingThrottledExporter(
            CurrentNetworkProvider currentNetworkProvider) {
        String endpoint = getEndpoint();
        SpanExporter zipkinSpanExporter = getCoreSpanExporter(endpoint);
        return ThrottlingExporter.newBuilder(
                        new MemoryBufferingExporter(currentNetworkProvider, zipkinSpanExporter))
                .categorizeByAttribute(COMPONENT_KEY)
                .maxSpansInWindow(100)
                .windowSize(Duration.ofSeconds(30))
                .build();
    }

    SpanExporter getToDiskExporter() {
        return new LazyInitSpanExporter(
                () ->
                        ZipkinWriteToDiskExporterFactory.create(
                                application, builder.maxUsageMegabytes));
    }

    // visible for testing
    SpanExporter getCoreSpanExporter(String endpoint) {
        // return a lazy init exporter so the main thread doesn't block on the setup.
        return new LazyInitSpanExporter(
                () ->
                        ZipkinSpanExporter.builder()
                                .setEncoder(new CustomZipkinEncoder())
                                .setEndpoint(endpoint)
                                // remove the local IP address
                                .setLocalIpAddressSupplier(() -> null)
                                .build());
    }

    private static class LazyInitSpanExporter implements SpanExporter {
        @Nullable private volatile SpanExporter delegate;
        private final Supplier<SpanExporter> s;

        public LazyInitSpanExporter(Supplier<SpanExporter> s) {
            this.s = s;
        }

        private SpanExporter getDelegate() {
            SpanExporter d = delegate;
            if (d == null) {
                synchronized (this) {
                    d = delegate;
                    if (d == null) {
                        delegate = d = s.get();
                    }
                }
            }
            return d;
        }

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            return getDelegate().export(spans);
        }

        @Override
        public CompletableResultCode flush() {
            return getDelegate().flush();
        }

        @Override
        public CompletableResultCode shutdown() {
            return getDelegate().shutdown();
        }
    }
}
