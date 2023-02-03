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
import static com.splunk.rum.SplunkRum.COMPONENT_ERROR;
import static com.splunk.rum.SplunkRum.COMPONENT_KEY;
import static com.splunk.rum.SplunkRum.RUM_VERSION_KEY;
import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
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
import io.opentelemetry.rum.internal.instrumentation.anr.AnrDetector;
import io.opentelemetry.rum.internal.instrumentation.crash.CrashReporter;
import io.opentelemetry.rum.internal.instrumentation.network.CurrentNetworkProvider;
import io.opentelemetry.rum.internal.instrumentation.network.NetworkAttributesSpanAppender;
import io.opentelemetry.rum.internal.instrumentation.network.NetworkChangeMonitor;
import io.opentelemetry.rum.internal.instrumentation.slowrendering.SlowRenderingDetector;
import io.opentelemetry.sdk.common.Clock;
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
    private final AnchoredClock timingClock;

    RumInitializer(
            SplunkRumBuilder builder, Application application, AppStartupTimer startupTimer) {
        this.builder = builder;
        this.application = application;
        this.startupTimer = startupTimer;
        this.timingClock = startupTimer.startupClock;
    }

    SplunkRum initialize(
            Function<Application, CurrentNetworkProvider> currentNetworkProviderFactory,
            Looper mainLooper) {
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();

        long startTimeNanos = timingClock.now();
        OpenTelemetryRumBuilder otelRumBuilder = OpenTelemetryRum.builder();

        otelRumBuilder.setResource(createResource());
        initializationEvents.add(
                new RumInitializer.InitializationEvent("resourceInitialized", timingClock.now()));

        CurrentNetworkProvider currentNetworkProvider =
                currentNetworkProviderFactory.apply(application);
        initializationEvents.add(
                new InitializationEvent("connectionUtilInitialized", timingClock.now()));

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
                                    "attributeAppenderInitialized", timingClock.now()));

                    SpanExporter zipkinExporter = buildFilteringExporter(currentNetworkProvider);
                    initializationEvents.add(
                            new RumInitializer.InitializationEvent(
                                    "exporterInitialized", timingClock.now()));

                    BatchSpanProcessor batchSpanProcessor =
                            BatchSpanProcessor.builder(zipkinExporter).build();
                    initializationEvents.add(
                            new RumInitializer.InitializationEvent(
                                    "batchSpanProcessorInitialized", timingClock.now()));

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
                                        "debugSpanExporterInitialized", timingClock.now()));
                    }

                    initializationEvents.add(
                            new RumInitializer.InitializationEvent(
                                    "tracerProviderInitialized", timingClock.now()));
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

        otelRumBuilder.addInstrumentation(
                instrumentedApplication -> {
                    Tracer tracer =
                            instrumentedApplication
                                    .getOpenTelemetrySdk()
                                    .getTracer(SplunkRum.RUM_TRACER_NAME);
                    Application.ActivityLifecycleCallbacks activityCallbacks;
                    if (Build.VERSION.SDK_INT < 29) {
                        activityCallbacks =
                                new Pre29ActivityCallbacks(
                                        tracer, visibleScreenTracker, startupTimer);
                    } else {
                        activityCallbacks =
                                new ActivityCallbacks(tracer, visibleScreenTracker, startupTimer);
                    }
                    instrumentedApplication
                            .getApplication()
                            .registerActivityLifecycleCallbacks(activityCallbacks);
                    initializationEvents.add(
                            new RumInitializer.InitializationEvent(
                                    "activityLifecycleCallbacksInitialized", timingClock.now()));
                });

        OpenTelemetryRum openTelemetryRum = otelRumBuilder.build(application);

        recordInitializationSpans(
                startTimeNanos,
                initializationEvents,
                openTelemetryRum.getOpenTelemetry().getTracer(SplunkRum.RUM_TRACER_NAME));

        return new SplunkRum(openTelemetryRum, globalAttributesSpanAppender);
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
                            new InitializationEvent("anrMonitorInitialized", timingClock.now()));
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
                                    "networkMonitorInitialized", timingClock.now()));
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
                                    "slowRenderingDetectorInitialized", timingClock.now()));
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
                                    "crashReportingInitialized", timingClock.now()));
                });
    }

    private void recordInitializationSpans(
            long startTimeNanos, List<InitializationEvent> initializationEvents, Tracer tracer) {
        Span overallAppStart = startupTimer.start(tracer);
        Span span =
                tracer.spanBuilder("SplunkRum.initialize")
                        .setParent(Context.current().with(overallAppStart))
                        .setStartTimestamp(startTimeNanos, TimeUnit.NANOSECONDS)
                        .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_APPSTART)
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
        long spanEndTime = timingClock.now();
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
                new InitializationEvent("zipkin exporter initialized", timingClock.now()));
        return filteredExporter;
    }

    private SpanExporter buildExporter(CurrentNetworkProvider currentNetworkProvider) {
        if (builder.debugEnabled) {
            // tell the Zipkin exporter to shut up already. We're on mobile, network stuff happens.
            // we'll do our best to hang on to the spans with the wrapping BufferingExporter.
            ZipkinSpanExporter.baseLogger.setLevel(Level.SEVERE);
            initializationEvents.add(
                    new InitializationEvent("logger setup complete", timingClock.now()));
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
                .categorizeByAttribute(SplunkRum.COMPONENT_KEY)
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

    // copied from otel-java
    static final class AnchoredClock {
        private final Clock clock;
        private final long epochNanos;
        private final long nanoTime;

        private AnchoredClock(Clock clock, long epochNanos, long nanoTime) {
            this.clock = clock;
            this.epochNanos = epochNanos;
            this.nanoTime = nanoTime;
        }

        public static AnchoredClock create(Clock clock) {
            return new AnchoredClock(clock, clock.now(), clock.nanoTime());
        }

        long now() {
            long deltaNanos = this.clock.nanoTime() - this.nanoTime;
            return this.epochNanos + deltaNanos;
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
