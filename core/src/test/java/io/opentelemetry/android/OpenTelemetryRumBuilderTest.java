/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_ID;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig;
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter;
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader;
import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.android.instrumentation.internal.AndroidInstrumentationLoaderImpl;
import io.opentelemetry.android.internal.initialization.InitializationEvents;
import io.opentelemetry.android.internal.services.CacheStorage;
import io.opentelemetry.android.internal.services.Preferences;
import io.opentelemetry.android.internal.services.Services;
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle;
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker;
import io.opentelemetry.android.internal.session.SessionIdTimeoutHandler;
import io.opentelemetry.android.session.SessionManager;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.incubator.events.EventLogger;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.disk.buffering.SpanToDiskExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.logs.internal.SdkEventLoggerProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class OpenTelemetryRumBuilderTest {

    public static final String CUR_SCREEN_NAME = "Celebratory Token";
    final Resource resource =
            Resource.getDefault().toBuilder().put("test.attribute", "abcdef").build();
    final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
    final InMemoryLogRecordExporter logsExporter = InMemoryLogRecordExporter.create();
    final InMemoryLogRecordExporter logRecordExporter = InMemoryLogRecordExporter.create();

    @Mock Application application;
    @Mock Looper looper;
    @Mock android.content.Context applicationContext;

    @Mock InitializationEvents initializationEvents;
    private AutoCloseable mocks;

    @Before
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        when(application.getApplicationContext()).thenReturn(applicationContext);
        when(application.getMainLooper()).thenReturn(looper);
        InitializationEvents.set(initializationEvents);
    }

    @After
    public void tearDown() throws Exception {
        SignalFromDiskExporter.resetForTesting();
        InitializationEvents.resetForTest();
        AndroidInstrumentationLoader.resetForTest();
        mocks.close();
    }

    @Test
    public void shouldRegisterApplicationStateWatcher() {
        Services services = createServiceManager();
        AppLifecycle appLifecycle = services.getAppLifecycle();

        makeBuilder().setServiceManager(services).build();

        verify(appLifecycle).registerListener(isA(ApplicationStateListener.class));
    }

    @Test
    public void shouldBuildTracerProvider() {
        OpenTelemetryRum openTelemetryRum =
                makeBuilder()
                        .setResource(resource)
                        .addTracerProviderCustomizer(
                                (tracerProviderBuilder, app) ->
                                        tracerProviderBuilder.addSpanProcessor(
                                                SimpleSpanProcessor.create(spanExporter)))
                        .build();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(
                        () -> {
                            String sessionId = openTelemetryRum.getRumSessionId();
                            openTelemetryRum
                                    .getOpenTelemetry()
                                    .getTracer("test")
                                    .spanBuilder("test span")
                                    .startSpan()
                                    .end();

                            List<SpanData> spans = spanExporter.getFinishedSpanItems();
                            assertThat(spans).hasSize(1);
                            assertThat(spans.get(0))
                                    .hasName("test span")
                                    .hasResource(resource)
                                    .hasAttributesSatisfyingExactly(
                                            equalTo(SESSION_ID, sessionId),
                                            equalTo(SCREEN_NAME_KEY, "unknown"));
                        });
    }

    @Test
    public void shouldBuildLogRecordProvider() {
        Services services = createServiceManager();
        OpenTelemetryRum openTelemetryRum =
                makeBuilder()
                        .setServiceManager(services)
                        .setResource(resource)
                        .addLoggerProviderCustomizer(
                                (logRecordProviderBuilder, app) ->
                                        logRecordProviderBuilder.addLogRecordProcessor(
                                                SimpleLogRecordProcessor.create(logsExporter)))
                        .build();

        OpenTelemetrySdk sdk = (OpenTelemetrySdk) openTelemetryRum.getOpenTelemetry();
        EventLogger eventLogger =
                SdkEventLoggerProvider.create(sdk.getSdkLoggerProvider())
                        .get("otel.initialization.events");
        Attributes attrs = Attributes.of(stringKey("mega"), "hit");
        eventLogger.builder("test.event").put("body.field", "foo").setAttributes(attrs).emit();

        List<LogRecordData> logs = logsExporter.getFinishedLogRecordItems();
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0))
                .hasAttributesSatisfyingExactly(
                        equalTo(SESSION_ID, openTelemetryRum.getRumSessionId()),
                        equalTo(stringKey("event.name"), "test.event"),
                        equalTo(SCREEN_NAME_KEY, CUR_SCREEN_NAME),
                        equalTo(stringKey("mega"), "hit"))
                .hasResource(resource);

        Value<?> bodyValue = logs.get(0).getBodyValue();
        List<KeyValue> payload = (List<KeyValue>) bodyValue.getValue();
        assertThat(payload).hasSize(1);
        KeyValue expected = KeyValue.of("body.field", Value.of("foo"));
        assertThat(payload.get(0)).isEqualTo(expected);
    }

    @Test
    public void shouldInstallInstrumentation() {
        Services services = createServiceManager();
        SessionManager sessionManager = mock();
        SessionIdTimeoutHandler timeoutHandler = mock();
        AndroidInstrumentation localInstrumentation = mock();
        AndroidInstrumentation classpathInstrumentation = mock();
        AndroidInstrumentationLoaderImpl androidInstrumentationServices =
                (AndroidInstrumentationLoaderImpl) AndroidInstrumentationLoader.get();
        androidInstrumentationServices.registerForTest(classpathInstrumentation);

        OpenTelemetryRum rum =
                new OpenTelemetryRumBuilder(application, buildConfig(), timeoutHandler)
                        .addInstrumentation(localInstrumentation)
                        .setServiceManager(services)
                        .setSessionManager(sessionManager)
                        .build();

        verify(services.getAppLifecycle()).registerListener(timeoutHandler);

        InstallationContext expectedCtx =
                new InstallationContext(
                        application, rum.getOpenTelemetry(), sessionManager, services);
        verify(localInstrumentation).install(eq(expectedCtx));
        verify(classpathInstrumentation).install(eq(expectedCtx));
    }

    @Test
    public void shouldInstallInstrumentation_excludingClasspathImplsWhenRequestedInConfig() {
        Services services = createServiceManager();
        SessionManager sessionManager = mock();
        SessionIdTimeoutHandler timeoutHandler = mock();
        AndroidInstrumentation localInstrumentation = mock();
        AndroidInstrumentation classpathInstrumentation = mock();
        AndroidInstrumentationLoaderImpl androidInstrumentationServices =
                (AndroidInstrumentationLoaderImpl) AndroidInstrumentationLoader.get();
        androidInstrumentationServices.registerForTest(classpathInstrumentation);

        OpenTelemetryRum rum =
                new OpenTelemetryRumBuilder(
                                application,
                                buildConfig().disableInstrumentationDiscovery(),
                                timeoutHandler)
                        .addInstrumentation(localInstrumentation)
                        .setServiceManager(services)
                        .setSessionManager(sessionManager)
                        .build();

        verify(services.getAppLifecycle()).registerListener(timeoutHandler);

        InstallationContext expectedCtx =
                new InstallationContext(
                        application, rum.getOpenTelemetry(), sessionManager, services);
        verify(localInstrumentation).install(eq(expectedCtx));
        verifyNoInteractions(classpathInstrumentation);
    }

    @Test
    public void canAddPropagator() {
        Context context = Context.root();
        Object carrier = new Object();

        Context expected = mock(Context.class);
        TextMapGetter<? super Object> getter = mock(TextMapGetter.class);
        TextMapPropagator customPropagator = mock(TextMapPropagator.class);

        when(customPropagator.extract(context, carrier, getter)).thenReturn(expected);

        OpenTelemetryRum rum = makeBuilder().addPropagatorCustomizer(x -> customPropagator).build();
        Context result =
                rum.getOpenTelemetry()
                        .getPropagators()
                        .getTextMapPropagator()
                        .extract(context, carrier, getter);
        assertThat(result).isSameAs(expected);
    }

    @Test
    public void canSetPropagator() {
        TextMapPropagator customPropagator = mock(TextMapPropagator.class);

        OpenTelemetryRum rum = makeBuilder().addPropagatorCustomizer(x -> customPropagator).build();
        TextMapPropagator result = rum.getOpenTelemetry().getPropagators().getTextMapPropagator();
        assertThat(result).isSameAs(customPropagator);
    }

    @Test
    public void setSpanExporterCustomizer() {
        SpanExporter exporter = mock(SpanExporter.class);
        AtomicBoolean wasCalled = new AtomicBoolean(false);
        Function<SpanExporter, SpanExporter> customizer =
                x -> {
                    wasCalled.set(true);
                    return exporter;
                };
        OpenTelemetryRum rum = makeBuilder().addSpanExporterCustomizer(customizer).build();
        Span span = rum.getOpenTelemetry().getTracer("test").spanBuilder("foo").startSpan();
        try (Scope scope = span.makeCurrent()) {
            // no-op
        } finally {
            span.end();
        }
        // 5 sec is default
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> verify(exporter).export(anyCollection()));
        assertThat(wasCalled.get()).isTrue();
    }

    @Test
    public void setLogRecordExporterCustomizer() {
        Services services = createServiceManager();
        AtomicBoolean wasCalled = new AtomicBoolean(false);
        Function<LogRecordExporter, LogRecordExporter> customizer =
                x -> {
                    wasCalled.set(true);
                    return logsExporter;
                };
        OpenTelemetryRum rum =
                makeBuilder()
                        .setServiceManager(services)
                        .addLogRecordExporterCustomizer(customizer)
                        .build();

        Logger logger = rum.getOpenTelemetry().getLogsBridge().loggerBuilder("LogScope").build();
        logger.logRecordBuilder()
                .setBody("foo")
                .setSeverity(Severity.FATAL3)
                .setAttribute(stringKey("bing"), "bang")
                .emit();
        // 5 sec is default
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(
                        () -> assertThat(logsExporter.getFinishedLogRecordItems()).isNotEmpty());
        assertThat(wasCalled.get()).isTrue();
        Collection<LogRecordData> logs = logsExporter.getFinishedLogRecordItems();
        assertThat(logs).hasSize(1);
        assertThat(logs.iterator().next())
                .hasBody("foo")
                .hasAttributesSatisfyingExactly(
                        equalTo(stringKey("bing"), "bang"),
                        equalTo(SCREEN_NAME_KEY, CUR_SCREEN_NAME),
                        equalTo(SESSION_ID, rum.getRumSessionId()))
                .hasSeverity(Severity.FATAL3);
    }

    @Test
    public void diskBufferingEnabled() {
        Services services = createServiceManager();
        CacheStorage cacheStorage = services.getCacheStorage();
        doReturn(60 * 1024 * 1024L).when(cacheStorage).ensureCacheSpaceAvailable(anyLong());
        OtelRumConfig config = buildConfig();
        ExportScheduleHandler scheduleHandler = mock();
        config.setDiskBufferingConfig(new DiskBufferingConfig(true));
        ArgumentCaptor<SpanExporter> exporterCaptor = ArgumentCaptor.forClass(SpanExporter.class);

        OpenTelemetryRum.builder(application, config)
                .setExportScheduleHandler(scheduleHandler)
                .setServiceManager(services)
                .build();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(
                        () -> {
                            assertThat(SignalFromDiskExporter.get()).isNotNull();
                            verify(scheduleHandler).enable();
                            verify(scheduleHandler, never()).disable();
                            verify(initializationEvents)
                                    .spanExporterInitialized(exporterCaptor.capture());
                            assertThat(exporterCaptor.getValue())
                                    .isInstanceOf(SpanToDiskExporter.class);
                        });
    }

    @Test
    public void diskBufferingEnabled_when_exception_thrown() {
        Services services = createServiceManager();
        CacheStorage cacheStorage = services.getCacheStorage();
        ExportScheduleHandler scheduleHandler = mock();
        doReturn(60 * 1024 * 1024L).when(cacheStorage).ensureCacheSpaceAvailable(anyLong());
        doAnswer(
                        invocation -> {
                            throw new IOException();
                        })
                .when(cacheStorage)
                .getCacheDir();
        ArgumentCaptor<SpanExporter> exporterCaptor = ArgumentCaptor.forClass(SpanExporter.class);
        OtelRumConfig config = buildConfig();
        config.setDiskBufferingConfig(new DiskBufferingConfig(true));

        OpenTelemetryRum.builder(application, config)
                .setServiceManager(services)
                .setExportScheduleHandler(scheduleHandler)
                .build();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(
                        () -> {
                            verify(initializationEvents)
                                    .spanExporterInitialized(exporterCaptor.capture());
                            verify(scheduleHandler, never()).enable();
                            verify(scheduleHandler).disable();
                            assertThat(exporterCaptor.getValue())
                                    .isNotInstanceOf(SpanToDiskExporter.class);
                            assertThat(SignalFromDiskExporter.get()).isNull();
                        });
    }

    @Test
    public void sdkReadyListeners() {
        OtelRumConfig config = buildConfig();
        AtomicReference<OpenTelemetrySdk> seen = new AtomicReference<>();
        OpenTelemetryRum.builder(application, config)
                .addOtelSdkReadyListener(seen::set)
                .setServiceManager(createServiceManager())
                .build();
        assertThat(seen.get()).isNotNull();
    }

    @Test
    public void diskBufferingDisabled() {
        ArgumentCaptor<SpanExporter> exporterCaptor = ArgumentCaptor.forClass(SpanExporter.class);
        ExportScheduleHandler scheduleHandler = mock();

        OtelRumConfig config = buildConfig();
        config.setDiskBufferingConfig(new DiskBufferingConfig(false));

        OpenTelemetryRum.builder(application, config)
                .setExportScheduleHandler(scheduleHandler)
                .build();

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(
                        () -> {
                            verify(initializationEvents)
                                    .spanExporterInitialized(exporterCaptor.capture());
                            verify(scheduleHandler, never()).enable();
                            verify(scheduleHandler).disable();
                            assertThat(exporterCaptor.getValue())
                                    .isNotInstanceOf(SpanToDiskExporter.class);
                            assertThat(SignalFromDiskExporter.get()).isNull();
                        });
    }

    @Test
    public void verifyGlobalAttrsForLogs() {
        Services services = createServiceManager();
        OtelRumConfig otelRumConfig = buildConfig();
        otelRumConfig.setGlobalAttributes(
                () -> Attributes.of(stringKey("someGlobalKey"), "someGlobalValue"));

        OpenTelemetryRum rum =
                OpenTelemetryRum.builder(application, otelRumConfig)
                        .setServiceManager(services)
                        .addLoggerProviderCustomizer(
                                (sdkLoggerProviderBuilder, application) ->
                                        sdkLoggerProviderBuilder.addLogRecordProcessor(
                                                SimpleLogRecordProcessor.create(logRecordExporter)))
                        .build();

        Logger logger = rum.getOpenTelemetry().getLogsBridge().loggerBuilder("LogScope").build();
        logger.logRecordBuilder().setAttribute(stringKey("localAttrKey"), "localAttrValue").emit();

        List<LogRecordData> recordedLogs = logRecordExporter.getFinishedLogRecordItems();
        assertThat(recordedLogs).hasSize(1);
        LogRecordData logRecordData = recordedLogs.get(0);
        OpenTelemetryAssertions.assertThat(logRecordData)
                .hasAttributes(
                        Attributes.builder()
                                .put(SESSION_ID, rum.getRumSessionId())
                                .put("someGlobalKey", "someGlobalValue")
                                .put("localAttrKey", "localAttrValue")
                                .put(SCREEN_NAME_KEY, CUR_SCREEN_NAME)
                                .build());
    }

    @Test
    public void verifyDefaultServicesAreCreated() {
        AtomicReference<Services> serviceManagerHolder = new AtomicReference<>();
        AndroidInstrumentation instrumentationTrap =
                ctx -> serviceManagerHolder.set(ctx.getServices());
        makeBuilder().addInstrumentation(instrumentationTrap).build();
        assertThat(serviceManagerHolder.get()).isNotNull();
        assertThat(serviceManagerHolder.get().getAppLifecycle()).isNotNull();
        assertThat(serviceManagerHolder.get().getCacheStorage()).isNotNull();
        assertThat(serviceManagerHolder.get().getCurrentNetworkProvider()).isNotNull();
        assertThat(serviceManagerHolder.get().getPeriodicWork()).isNotNull();
        assertThat(serviceManagerHolder.get().getPreferences()).isNotNull();
        assertThat(serviceManagerHolder.get().getVisibleScreenTracker()).isNotNull();
    }

    @Test
    public void verifyServiceManagerIsStarted() {
        Services services = createServiceManager();
        makeBuilder().setServiceManager(services).build();
        verify(services).start();
    }

    /**
     * @noinspection KotlinInternalInJava
     */
    private static Services createServiceManager() {
        Services services = mock(Services.class);
        when(services.getAppLifecycle()).thenReturn(mock(AppLifecycle.class));
        when(services.getCacheStorage()).thenReturn(mock(CacheStorage.class));
        when(services.getPreferences()).thenReturn(mock(Preferences.class));
        VisibleScreenTracker screenService = mock(VisibleScreenTracker.class);
        when(screenService.getCurrentlyVisibleScreen()).thenReturn(CUR_SCREEN_NAME);
        when(services.getVisibleScreenTracker()).thenReturn(screenService);
        return services;
    }

    @NonNull
    private OpenTelemetryRumBuilder makeBuilder() {
        return OpenTelemetryRum.builder(application, buildConfig());
    }

    private OtelRumConfig buildConfig() {
        return new OtelRumConfig().disableNetworkAttributes().disableSdkInitializationEvents();
    }
}
