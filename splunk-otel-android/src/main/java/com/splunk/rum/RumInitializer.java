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

import android.app.Application;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.splunk.android.rum.R;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.Clock;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

class RumInitializer {

    private final Config config;
    private final Application application;
    private final AppStartupTimer startupTimer;

    RumInitializer(Config config, Application application, AppStartupTimer startupTimer) {
        this.config = config;
        this.application = application;
        this.startupTimer = startupTimer;
    }

    SplunkRum initialize(Supplier<ConnectionUtil> connectionUtilSupplier, Looper mainLooper) {
        String rumVersion = detectRumVersion();
        VisibleScreenTracker visibleScreenTracker = new VisibleScreenTracker();

        Clock baseClock = Clock.getDefault();
        AnchoredClock timingClock = AnchoredClock.create(baseClock);

        long startTimeNanos = timingClock.now();
        List<RumInitializer.InitializationEvent> initializationEvents = new ArrayList<>();

        ConnectionUtil connectionUtil = connectionUtilSupplier.get();
        initializationEvents.add(new InitializationEvent("connectionUtilInitialized", timingClock.now()));

        SpanExporter zipkinExporter = buildExporter(connectionUtil);
        initializationEvents.add(new RumInitializer.InitializationEvent("exporterInitialized", timingClock.now()));

        SessionId sessionId = new SessionId();
        initializationEvents.add(new RumInitializer.InitializationEvent("sessionIdInitialized", timingClock.now()));

        SdkTracerProvider sdkTracerProvider = buildTracerProvider(baseClock, zipkinExporter, sessionId, rumVersion, visibleScreenTracker, connectionUtil);
        initializationEvents.add(new RumInitializer.InitializationEvent("tracerProviderInitialized", timingClock.now()));

        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder().setTracerProvider(sdkTracerProvider).build();
        initializationEvents.add(new RumInitializer.InitializationEvent("openTelemetrySdkInitialized", timingClock.now()));

        Tracer tracer = openTelemetrySdk.getTracer(SplunkRum.RUM_TRACER_NAME);
        if (Build.VERSION.SDK_INT < 29) {
            application.registerActivityLifecycleCallbacks(new Pre29ActivityCallbacks(tracer, visibleScreenTracker, startupTimer));
        } else {
            application.registerActivityLifecycleCallbacks(new ActivityCallbacks(tracer, visibleScreenTracker, startupTimer));
        }
        initializationEvents.add(new RumInitializer.InitializationEvent("activityLifecycleCallbacksInitialized", timingClock.now()));

        if (config.isCrashReportingEnabled()) {
            CrashReporter.initializeCrashReporting(tracer, openTelemetrySdk);
            initializationEvents.add(new RumInitializer.InitializationEvent("crashReportingInitialized", timingClock.now()));
        }

        if (config.isNetworkMonitorEnabled()) {
            new NetworkMonitor(connectionUtil).addConnectivityListener(tracer);
            initializationEvents.add(new RumInitializer.InitializationEvent("networkMonitorInitialized", timingClock.now()));
        }

        if (config.isAnrDetectionEnabled()) {
            initializeAnrReporting(mainLooper);
            initializationEvents.add(new RumInitializer.InitializationEvent("anrMonitorInitialized", timingClock.now()));
        }

        recordInitializationSpans(startTimeNanos, initializationEvents, tracer, config);

        return new SplunkRum(openTelemetrySdk, sessionId, config);
    }

    private void initializeAnrReporting(Looper mainLooper) {
        Thread mainThread = mainLooper.getThread();
        Handler uiHandler = new Handler(mainLooper);
        AnrWatcher anrWatcher = new AnrWatcher(uiHandler, mainThread, SplunkRum::getInstance);
        Executors.newScheduledThreadPool(1)
                .scheduleAtFixedRate(anrWatcher, 1, 1, TimeUnit.SECONDS);
    }

    private String detectRumVersion() {
        try {
            //todo: figure out if there's a way to get access to resources from pure non-UI library code.
            return application.getApplicationContext().getResources().getString(R.string.rum_version);
        } catch (Exception e) {
            //ignore for now
        }
        return "unknown";
    }

    private void recordInitializationSpans(long startTimeNanos, List<InitializationEvent> initializationEvents, Tracer tracer, Config config) {
        Span overallAppStart = startupTimer.start(tracer);
        Span span = tracer.spanBuilder("SplunkRum.initialize")
                .setParent(Context.current().with(overallAppStart))
                .setStartTimestamp(startTimeNanos, TimeUnit.NANOSECONDS)
                .setAttribute(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_APPSTART)
                .startSpan();

        String configSettings = "[debug:" + config.isDebugEnabled() + "," +
                "crashReporting:" + config.isCrashReportingEnabled() + "," +
                "anrReporting:" + config.isAnrDetectionEnabled() + "," +
                "networkMonitor:" + config.isNetworkMonitorEnabled() + "]";
        span.setAttribute("config_settings", configSettings);

        for (RumInitializer.InitializationEvent initializationEvent : initializationEvents) {
            span.addEvent(initializationEvent.name, initializationEvent.time, TimeUnit.NANOSECONDS);
        }
        span.end();
    }

    private SdkTracerProvider buildTracerProvider(
            Clock clock,
            SpanExporter zipkinExporter,
            SessionId sessionId,
            String rumVersion,
            VisibleScreenTracker visibleScreenTracker,
            ConnectionUtil connectionUtil) {
        SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder()
                .setClock(clock)
                .addSpanProcessor(BatchSpanProcessor.builder(zipkinExporter).build())
                .addSpanProcessor(new RumAttributeAppender(config, sessionId, rumVersion, visibleScreenTracker, connectionUtil))
                .setResource(Resource.getDefault().toBuilder().put("service.name", config.getApplicationName()).build());
        if (config.isDebugEnabled()) {
            tracerProviderBuilder.addSpanProcessor(SimpleSpanProcessor.create(new LoggingSpanExporter()));
        }
        return tracerProviderBuilder
                .build();
    }

    SpanExporter buildExporter(ConnectionUtil connectionUtil) {
        String endpoint = config.getBeaconEndpoint() + "?auth=" + config.getRumAccessToken();
        if (!config.isDebugEnabled()) {
            //tell the Zipkin exporter to shut up already. We're on mobile, network stuff happens.
            // we'll do our best to hang on to the spans with the wrapping BufferingExporter.
            ZipkinSpanExporter.baseLogger.setLevel(Level.SEVERE);
        }
        ZipkinSpanExporter zipkinSpanExporter = ZipkinSpanExporter.builder()
                .setEncoder(new CustomZipkinEncoder())
                .setEndpoint(endpoint).build();
        SpanExporter throttlingExporter = ThrottlingExporter.newBuilder(zipkinSpanExporter)
                .categorizeByAttribute(SplunkRum.COMPONENT_KEY)
                .maxSpansInWindow(100)
                .windowSize(Duration.ofSeconds(30))
                .build();
        return new BufferingExporter(connectionUtil, throttlingExporter);
    }

    static class InitializationEvent {
        private final String name;
        private final long time;

        private InitializationEvent(String name, long time) {
            this.name = name;
            this.time = time;
        }
    }

    //copied from otel-java
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

}
