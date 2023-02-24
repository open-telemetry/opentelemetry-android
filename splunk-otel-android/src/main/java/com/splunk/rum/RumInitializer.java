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
import static com.splunk.rum.SplunkRum.RUM_VERSION_KEY;
import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
import static io.opentelemetry.rum.internal.RumConstants.APP_START_SPAN_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEPLOYMENT_ENVIRONMENT;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEVICE_MODEL_IDENTIFIER;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEVICE_MODEL_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_TYPE;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_VERSION;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.SERVICE_NAME;
import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.os.Build;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.splunk.android.rum.R;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.rum.internal.GlobalAttributesSpanAppender;
import io.opentelemetry.rum.internal.OpenTelemetryRum;
import io.opentelemetry.rum.internal.OpenTelemetryRumBuilder;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private final List<RumInitializer.InitializationEvent> initializationEvents = new ArrayList<>();

    RumInitializer(
            SplunkRumBuilder builder, Application application, AppStartupTimer startupTimer) {
        this.builder = builder;
        this.application = application;
        this.startupTimer = startupTimer;
    }

    SplunkRum initialize(
            Function<Application, CurrentNetworkProvider> currentNetworkProviderFactory,
            Looper mainLooper) {
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();

        long startTimeNanos = startupTimer.clockNow();
        OpenTelemetryRumBuilder otelRumBuilder = OpenTelemetryRum.builder();

        otelRumBuilder.setResource(createResource());
        initializationEvents.add(
                new RumInitializer.InitializationEvent(
                        "resourceInitialized", startupTimer.clockNow()));

        CurrentNetworkProvider currentNetworkProvider =
                currentNetworkProviderFactory.apply(application);
        initializationEvents.add(
                new InitializationEvent("connectionUtilInitialized", startupTimer.clockNow()));

        GlobalAttributesSpanAppender globalAttributesSpanAppender =
                GlobalAttributesSpanAppender.create(builder.globalAttributes);
        otelRumBuilder.addTracerProviderCustomizer(
                (tracerProviderBuilder, app) -> {
                    SpanProcessor networkAttributesSpanAppender =
                            NetworkAttributesSpanAppender.create(currentNetworkProvider);
                    ScreenAttributesAppender screenAttributesAppender =
                            new ScreenAttributesAppender(visibleScreenTracker);
                    initializationEvents.add(
                            new RumInitializer.InitializationEvent(
                                    "attributeAppenderInitialized", startupTimer.clockNow()));

                    SpanExporter zipkinExporter = buildFilteringExporter(currentNetworkProvider);
                    initializationEvents.add(
                            new RumInitializer.InitializationEvent(
                                    "exporterInitialized", startupTimer.clockNow()));

                    BatchSpanProcessor batchSpanProcessor =
                            BatchSpanProcessor.builder(zipkinExporter).build();
                    initializationEvents.add(
                            new RumInitializer.InitializationEvent(
                                    "batchSpanProcessorInitialized", startupTimer.clockNow()));

                    tracerProviderBuilder
                            .addSpanProcessor(globalAttributesSpanAppender)
                            .addSpanProcessor(networkAttributesSpanAppender)
                            .addSpanProcessor(screenAttributesAppender)
                            .addSpanProcessor(batchSpanProcessor)
                            .setSpanLimits(
                                    SpanLimits.builder()
                                            .setMaxAttributeValueLength(MAX_ATTRIBUTE_LENGTH)
                                            .build());

                    if (builder.sessionBasedSamplerEnabled) {
                        // TODO: this is hacky behavior that utilizes a mutable variable, fix this!
                        tracerProviderBuilder.setSampler(
                                new SessionIdRatioBasedSampler(
                                        builder.sessionBasedSamplerRatio, SplunkRum::getInstance));
                    }

                    if (builder.debugEnabled) {
                        tracerProviderBuilder.addSpanProcessor(
                                SimpleSpanProcessor.create(
                                        builder.decorateWithSpanFilter(
                                                LoggingSpanExporter.create())));
                        initializationEvents.add(
                                new RumInitializer.InitializationEvent(
                                        "debugSpanExporterInitialized", startupTimer.clockNow()));
                    }

                    initializationEvents.add(
                            new RumInitializer.InitializationEvent(
                                    "tracerProviderInitialized", startupTimer.clockNow()));
                    return tracerProviderBuilder;
                });

        if (builder.anrDetectionEnabled) {
            installAnrDetector(otelRumBuilder, mainLooper);
        }
        if (builder.networkMonitorEnabled) {
            installNetworkMonitor(otelRumBuilder, currentNetworkProvider);
        }
        if (builder.slowRenderingDetectionEnabled) {
            installSlowRenderingDetector(otelRumBuilder);
        }
        if (builder.crashReportingEnabled) {
            installCrashReporter(otelRumBuilder);
        }

        // Lifecycle events instrumentation are always installed.
        installLifecycleInstrumentations(otelRumBuilder, visibleScreenTracker);

        OpenTelemetryRum openTelemetryRum = otelRumBuilder.build(application);

        recordInitializationSpans(
                startTimeNanos,
                initializationEvents,
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
                                    .build();
                    instrumentation.installOn(instrumentedApp);
                    initializationEvents.add(
                            new InitializationEvent(
                                    "activityLifecycleCallbacksInitialized",
                                    startupTimer.clockNow()));
                });
    }

    private Resource createResource() {
        // applicationName can't be null at this stage
        String applicationName = requireNonNull(builder.applicationName);
        ResourceBuilder resourceBuilder =
                Resource.getDefault().toBuilder()
                        .put(APP_NAME_KEY, applicationName)
                        .put(SERVICE_NAME, applicationName);
        if (builder.deploymentEnvironment != null) {
            resourceBuilder.put(DEPLOYMENT_ENVIRONMENT, builder.deploymentEnvironment);
        }
        return resourceBuilder
                .put(RUM_VERSION_KEY, detectRumVersion())
                .put(DEVICE_MODEL_NAME, Build.MODEL)
                .put(DEVICE_MODEL_IDENTIFIER, Build.MODEL)
                .put(OS_NAME, "Android")
                .put(OS_TYPE, "linux")
                .put(OS_VERSION, Build.VERSION.RELEASE)
                .build();
    }

    private String detectRumVersion() {
        try {
            // todo: figure out if there's a way to get access to resources from pure non-UI library
            // code.
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

                    initializationEvents.add(
                            new InitializationEvent(
                                    "anrMonitorInitialized", startupTimer.clockNow()));
                });
    }

    private void installNetworkMonitor(
            OpenTelemetryRumBuilder otelRumBuilder, CurrentNetworkProvider currentNetworkProvider) {
        otelRumBuilder.addInstrumentation(
                instrumentedApplication -> {
                    NetworkChangeMonitor.create(currentNetworkProvider)
                            .installOn(instrumentedApplication);
                    initializationEvents.add(
                            new InitializationEvent(
                                    "networkMonitorInitialized", startupTimer.clockNow()));
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
                    initializationEvents.add(
                            new InitializationEvent(
                                    "slowRenderingDetectorInitialized", startupTimer.clockNow()));
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

                    initializationEvents.add(
                            new InitializationEvent(
                                    "crashReportingInitialized", startupTimer.clockNow()));
                });
    }

    private void recordInitializationSpans(
            long startTimeNanos,
            List<InitializationEvent> initializationEvents,
            Tracer delegateTracer) {

        Tracer tracer =
                spanName ->
                        delegateTracer
                                .spanBuilder(spanName)
                                .setAttribute(COMPONENT_KEY, COMPONENT_APPSTART);

        Span overallAppStart = startupTimer.start(tracer);
        Span span =
                tracer.spanBuilder("SplunkRum.initialize")
                        .setParent(Context.current().with(overallAppStart))
                        .setStartTimestamp(startTimeNanos, TimeUnit.NANOSECONDS)
                        .setAttribute(COMPONENT_KEY, COMPONENT_APPSTART)
                        .startSpan();

        String configSettings =
                "[debug:"
                        + builder.debugEnabled
                        + ","
                        + "crashReporting:"
                        + builder.crashReportingEnabled
                        + ","
                        + "anrReporting:"
                        + builder.anrDetectionEnabled
                        + ","
                        + "slowRenderingDetector:"
                        + builder.slowRenderingDetectionEnabled
                        + ","
                        + "networkMonitor:"
                        + builder.networkMonitorEnabled
                        + "]";
        span.setAttribute("config_settings", configSettings);

        for (RumInitializer.InitializationEvent initializationEvent : initializationEvents) {
            span.addEvent(initializationEvent.name, initializationEvent.time, TimeUnit.NANOSECONDS);
        }
        long spanEndTime = startupTimer.clockNow();
        // we only want to create SplunkRum.initialize span when there is a AppStart span so we
        // register a callback that is called right before AppStart span is ended
        startupTimer.setCompletionCallback(() -> span.end(spanEndTime, TimeUnit.NANOSECONDS));
    }

    // visible for testing
    SpanExporter buildFilteringExporter(CurrentNetworkProvider currentNetworkProvider) {
        SpanExporter exporter = buildExporter(currentNetworkProvider);
        SpanExporter splunkTranslatedExporter =
                new SplunkSpanDataModifier(exporter, builder.reactNativeSupportEnabled);
        SpanExporter filteredExporter = builder.decorateWithSpanFilter(splunkTranslatedExporter);
        initializationEvents.add(
                new InitializationEvent("zipkin exporter initialized", startupTimer.clockNow()));
        return filteredExporter;
    }

    private SpanExporter buildExporter(CurrentNetworkProvider currentNetworkProvider) {
        if (builder.debugEnabled) {
            // tell the Zipkin exporter to shut up already. We're on mobile, network stuff happens.
            // we'll do our best to hang on to the spans with the wrapping BufferingExporter.
            ZipkinSpanExporter.baseLogger.setLevel(Level.SEVERE);
            initializationEvents.add(
                    new InitializationEvent("logger setup complete", startupTimer.clockNow()));
        }

        if (builder.diskBufferingEnabled) {
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

    static class InitializationEvent {
        private final String name;
        private final long time;

        private InitializationEvent(String name, long time) {
            this.name = name;
            this.time = time;
        }
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
