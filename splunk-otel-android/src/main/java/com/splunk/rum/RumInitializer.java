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

import static com.splunk.rum.SplunkRum.LOG_TAG;
import static java.util.Objects.requireNonNull;

import android.app.Application;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.splunk.android.rum.R;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanLimits;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

class RumInitializer {

    // we're setting a fairly large length limit to capture long stack traces; ~256 lines,
    // assuming 128 chars per line
    static final int MAX_ATTRIBUTE_LENGTH = 256 * 128;

    private final SplunkRumBuilder builder;
    private final AtomicReference<Attributes> globalAttributes;
    private final Application application;
    private final AppStartupTimer startupTimer;
    private final List<RumInitializer.InitializationEvent> initializationEvents = new ArrayList<>();
    private final AnchoredClock timingClock;

    RumInitializer(
            SplunkRumBuilder builder, Application application, AppStartupTimer startupTimer) {
        this.builder = builder;
        this.globalAttributes = builder.buildGlobalAttributesRef();
        this.application = application;
        this.startupTimer = startupTimer;
        this.timingClock = startupTimer.startupClock;
    }

    SplunkRum initialize(ConnectionUtil.Factory connectionUtilFactory, Looper mainLooper) {
        String rumVersion = detectRumVersion();
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();

        long startTimeNanos = timingClock.now();
        List<AppStateListener> appStateListeners = new ArrayList<>();

        ConnectionUtil connectionUtil = connectionUtilFactory.createAndStart(application);
        initializationEvents.add(
                new InitializationEvent("connectionUtilInitialized", timingClock.now()));

        SpanExporter zipkinExporter = buildFilteringExporter(connectionUtil);
        initializationEvents.add(
                new RumInitializer.InitializationEvent("exporterInitialized", timingClock.now()));

        SessionIdTimeoutHandler timeoutHandler = new SessionIdTimeoutHandler();
        SessionId sessionId = new SessionId(timeoutHandler);
        appStateListeners.add(timeoutHandler);
        initializationEvents.add(
                new RumInitializer.InitializationEvent("sessionIdInitialized", timingClock.now()));

        SdkTracerProvider sdkTracerProvider =
                buildTracerProvider(
                        Clock.getDefault(),
                        zipkinExporter,
                        sessionId,
                        rumVersion,
                        visibleScreenTracker,
                        connectionUtil);
        initializationEvents.add(
                new RumInitializer.InitializationEvent(
                        "tracerProviderInitialized", timingClock.now()));

        OpenTelemetrySdk openTelemetrySdk =
                OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).build();
        initializationEvents.add(
                new RumInitializer.InitializationEvent(
                        "openTelemetrySdkInitialized", timingClock.now()));

        if (builder.anrDetectionEnabled) {
            appStateListeners.add(initializeAnrReporting(mainLooper));
            initializationEvents.add(
                    new RumInitializer.InitializationEvent(
                            "anrMonitorInitialized", timingClock.now()));
        }

        Tracer tracer = openTelemetrySdk.getTracer(SplunkRum.RUM_TRACER_NAME);
        sessionId.setSessionIdChangeListener(new SessionIdChangeTracer(tracer));

        if (builder.networkMonitorEnabled) {
            NetworkMonitor networkMonitor = new NetworkMonitor(connectionUtil);
            networkMonitor.addConnectivityListener(tracer);
            appStateListeners.add(networkMonitor);
            initializationEvents.add(
                    new RumInitializer.InitializationEvent(
                            "networkMonitorInitialized", timingClock.now()));
        }

        SlowRenderingDetector slowRenderingDetector = buildSlowRenderingDetector(tracer);
        slowRenderingDetector.start();

        if (Build.VERSION.SDK_INT < 29) {
            application.registerActivityLifecycleCallbacks(
                    new Pre29ActivityCallbacks(
                            tracer, visibleScreenTracker, startupTimer, appStateListeners));
        } else {
            ActivityCallbacks activityCallbacks =
                    ActivityCallbacks.builder()
                            .tracer(tracer)
                            .visibleScreenTracker(visibleScreenTracker)
                            .startupTimer(startupTimer)
                            .appStateListeners(appStateListeners)
                            .slowRenderingDetector(slowRenderingDetector)
                            .build();
            application.registerActivityLifecycleCallbacks(activityCallbacks);
        }
        initializationEvents.add(
                new RumInitializer.InitializationEvent(
                        "activityLifecycleCallbacksInitialized", timingClock.now()));

        if (builder.crashReportingEnabled) {
            CrashReporter.initializeCrashReporting(tracer, openTelemetrySdk);
            initializationEvents.add(
                    new RumInitializer.InitializationEvent(
                            "crashReportingInitialized", timingClock.now()));
        }

        recordInitializationSpans(startTimeNanos, initializationEvents, tracer);

        return new SplunkRum(openTelemetrySdk, sessionId, globalAttributes);
    }

    private SlowRenderingDetector buildSlowRenderingDetector(Tracer tracer) {
        if (!builder.slowRenderingDetectionEnabled) {
            Log.w(LOG_TAG, "Slow/frozen rendering detection has been disabled by user.");
            return NoOpSlowRenderingDetector.INSTANCE;
        }
        try {
            initializationEvents.add(
                    new RumInitializer.InitializationEvent(
                            "slowRenderingDetectorInitialized", timingClock.now()));
            Class.forName("androidx.core.app.FrameMetricsAggregator");
            return new SlowRenderingDetectorImpl(
                    tracer, builder.slowRenderingDetectionPollInterval);
        } catch (ClassNotFoundException e) {
            Log.w(
                    LOG_TAG,
                    "FrameMetricsAggregator is not available on this platform - slow/frozen rendering detection is disabled.");
            return NoOpSlowRenderingDetector.INSTANCE;
        }
    }

    private AppStateListener initializeAnrReporting(Looper mainLooper) {
        Thread mainThread = mainLooper.getThread();
        Handler uiHandler = new Handler(mainLooper);
        AnrWatcher anrWatcher = new AnrWatcher(uiHandler, mainThread, SplunkRum::getInstance);
        ScheduledExecutorService anrScheduler = Executors.newScheduledThreadPool(1);
        final ScheduledFuture<?> scheduledFuture =
                anrScheduler.scheduleAtFixedRate(anrWatcher, 1, 1, TimeUnit.SECONDS);
        return new AppStateListener() {
            @Nullable private ScheduledFuture<?> future = scheduledFuture;

            @Override
            public void appForegrounded() {
                if (future == null) {
                    future = anrScheduler.scheduleAtFixedRate(anrWatcher, 1, 1, TimeUnit.SECONDS);
                }
            }

            @Override
            public void appBackgrounded() {
                if (future != null) {
                    future.cancel(true);
                    future = null;
                }
            }
        };
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

    private SdkTracerProvider buildTracerProvider(
            Clock clock,
            SpanExporter zipkinExporter,
            SessionId sessionId,
            String rumVersion,
            VisibleScreenTracker visibleScreenTracker,
            ConnectionUtil connectionUtil) {
        BatchSpanProcessor batchSpanProcessor = BatchSpanProcessor.builder(zipkinExporter).build();
        initializationEvents.add(
                new RumInitializer.InitializationEvent(
                        "batchSpanProcessorInitialized", timingClock.now()));

        String applicationName = requireNonNull(builder.applicationName);
        RumAttributeAppender attributeAppender =
                new RumAttributeAppender(
                        applicationName,
                        globalAttributes::get,
                        sessionId,
                        rumVersion,
                        visibleScreenTracker,
                        connectionUtil);
        initializationEvents.add(
                new RumInitializer.InitializationEvent(
                        "attributeAppenderInitialized", timingClock.now()));

        Resource resource =
                Resource.getDefault().toBuilder().put("service.name", applicationName).build();
        initializationEvents.add(
                new RumInitializer.InitializationEvent("resourceInitialized", timingClock.now()));

        SdkTracerProviderBuilder tracerProviderBuilder =
                SdkTracerProvider.builder()
                        .setClock(clock)
                        .addSpanProcessor(batchSpanProcessor)
                        .addSpanProcessor(attributeAppender)
                        .setSpanLimits(
                                SpanLimits.builder()
                                        .setMaxAttributeValueLength(MAX_ATTRIBUTE_LENGTH)
                                        .build())
                        .setResource(resource);
        initializationEvents.add(
                new RumInitializer.InitializationEvent(
                        "tracerProviderBuilderInitialized", timingClock.now()));

        if (builder.sessionBasedSamplerEnabled) {
            tracerProviderBuilder.setSampler(
                    new SessionIdRatioBasedSampler(builder.sessionBasedSamplerRatio, sessionId));
        }

        if (builder.debugEnabled) {
            tracerProviderBuilder.addSpanProcessor(
                    SimpleSpanProcessor.create(
                            builder.decorateWithSpanFilter(LoggingSpanExporter.create())));
            initializationEvents.add(
                    new RumInitializer.InitializationEvent(
                            "debugSpanExporterInitialized", timingClock.now()));
        }
        return tracerProviderBuilder.build();
    }

    // visible for testing
    SpanExporter buildFilteringExporter(ConnectionUtil connectionUtil) {
        SpanExporter exporter = buildExporter(connectionUtil);
        SpanExporter filteredExporter = builder.decorateWithSpanFilter(exporter);
        initializationEvents.add(
                new InitializationEvent("zipkin exporter initialized", timingClock.now()));
        return filteredExporter;
    }

    private SpanExporter buildExporter(ConnectionUtil connectionUtil) {
        if (builder.debugEnabled) {
            // tell the Zipkin exporter to shut up already. We're on mobile, network stuff happens.
            // we'll do our best to hang on to the spans with the wrapping BufferingExporter.
            ZipkinSpanExporter.baseLogger.setLevel(Level.SEVERE);
            initializationEvents.add(
                    new InitializationEvent("logger setup complete", timingClock.now()));
        }

        if (builder.diskBufferingEnabled) {
            return buildStorageBufferingExporter(connectionUtil);
        }

        return buildMemoryBufferingThrottledExporter(connectionUtil);
    }

    private SpanExporter buildStorageBufferingExporter(ConnectionUtil connectionUtil) {
        Sender sender = OkHttpSender.newBuilder().endpoint(getEndpoint()).build();
        File spanFilesPath = FileUtils.getSpansDirectory(application);
        BandwidthTracker bandwidthTracker = new BandwidthTracker();

        FileSender fileSender =
                FileSender.builder().sender(sender).bandwidthTracker(bandwidthTracker).build();
        DiskToZipkinExporter diskToZipkinExporter =
                DiskToZipkinExporter.builder()
                        .connectionUtil(connectionUtil)
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

    private SpanExporter buildMemoryBufferingThrottledExporter(ConnectionUtil connectionUtil) {
        String endpoint = getEndpoint();
        SpanExporter zipkinSpanExporter = getCoreSpanExporter(endpoint);
        return ThrottlingExporter.newBuilder(
                        new MemoryBufferingExporter(connectionUtil, zipkinSpanExporter))
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
