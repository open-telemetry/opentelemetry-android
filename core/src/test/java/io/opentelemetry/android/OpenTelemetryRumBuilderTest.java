/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.notNull;
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
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration;
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter;
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader;
import io.opentelemetry.android.internal.initialization.InitializationEvents;
import io.opentelemetry.android.internal.instrumentation.AndroidInstrumentationLoaderImpl;
import io.opentelemetry.android.internal.services.CacheStorage;
import io.opentelemetry.android.internal.services.Preferences;
import io.opentelemetry.android.internal.services.ServiceManager;
import io.opentelemetry.android.internal.services.ServiceManagerImpl;
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycleService;
import io.opentelemetry.android.internal.services.applifecycle.ApplicationStateListener;
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.incubator.events.EventLogger;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.LoggerBuilder;
import io.opentelemetry.api.logs.LoggerProvider;
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
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
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
        ServiceManager.resetForTest();
        mocks.close();
    }

    @Test
    public void shouldRegisterApplicationStateWatcher() {
        ServiceManager serviceManager = createServiceManager();
        AppLifecycleService appLifecycleService = serviceManager.getAppLifecycleService();

        makeBuilder().build(serviceManager);

        verify(appLifecycleService).registerListener(isA(ApplicationStateListener.class));
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
                                            equalTo(
                                                    SessionIncubatingAttributes.SESSION_ID,
                                                    sessionId),
                                            equalTo(SCREEN_NAME_KEY, "unknown"));
                        });
    }

    @Test
    public void shouldBuildLogRecordProvider() {
        OpenTelemetryRum openTelemetryRum =
                makeBuilder()
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
                        equalTo(stringKey("event.name"), "test.event"),
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
        ServiceManager serviceManager = createServiceManager();
        SessionIdTimeoutHandler timeoutHandler = mock();
        AndroidInstrumentation localInstrumentation = mock();
        AndroidInstrumentation classpathInstrumentation = mock();
        AndroidInstrumentationLoaderImpl androidInstrumentationServices =
                (AndroidInstrumentationLoaderImpl) AndroidInstrumentationLoader.get();
        androidInstrumentationServices.registerForTest(classpathInstrumentation);

        new OpenTelemetryRumBuilder(application, buildConfig(), timeoutHandler)
                .addInstrumentation(localInstrumentation)
                .build(serviceManager);

        verify(serviceManager.getAppLifecycleService()).registerListener(timeoutHandler);

        verify(localInstrumentation).install(eq(application), notNull());
        verify(classpathInstrumentation).install(eq(application), notNull());
    }

    @Test
    public void shouldInstallInstrumentation_excludingClasspathImplsWhenRequestedInConfig() {
        ServiceManager serviceManager = createServiceManager();
        SessionIdTimeoutHandler timeoutHandler = mock();
        AndroidInstrumentation localInstrumentation = mock();
        AndroidInstrumentation classpathInstrumentation = mock();
        AndroidInstrumentationLoaderImpl androidInstrumentationServices =
                (AndroidInstrumentationLoaderImpl) AndroidInstrumentationLoader.get();
        androidInstrumentationServices.registerForTest(classpathInstrumentation);

        new OpenTelemetryRumBuilder(
                        application,
                        buildConfig().disableInstrumentationDiscovery(),
                        timeoutHandler)
                .addInstrumentation(localInstrumentation)
                .build(serviceManager);

        verify(serviceManager.getAppLifecycleService()).registerListener(timeoutHandler);

        verify(localInstrumentation).install(eq(application), notNull());
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
        AtomicBoolean wasCalled = new AtomicBoolean(false);
        Function<LogRecordExporter, LogRecordExporter> customizer =
                x -> {
                    wasCalled.set(true);
                    return logsExporter;
                };
        OpenTelemetryRum rum = makeBuilder().addLogRecordExporterCustomizer(customizer).build();

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
                .hasAttributesSatisfyingExactly(equalTo(stringKey("bing"), "bang"))
                .hasSeverity(Severity.FATAL3);
    }

    @Test
    public void diskBufferingEnabled() {
        ServiceManager serviceManager = createServiceManager();
        CacheStorage cacheStorage = serviceManager.getCacheStorage();
        doReturn(60 * 1024 * 1024L).when(cacheStorage).ensureCacheSpaceAvailable(anyLong());
        OtelRumConfig config = buildConfig();
        ExportScheduleHandler scheduleHandler = mock();
        config.setDiskBufferingConfiguration(
                DiskBufferingConfiguration.builder()
                        .setEnabled(true)
                        .setExportScheduleHandler(scheduleHandler)
                        .build());
        ArgumentCaptor<SpanExporter> exporterCaptor = ArgumentCaptor.forClass(SpanExporter.class);

        OpenTelemetryRum.builder(application, config).build(serviceManager);

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
        ServiceManager serviceManager = createServiceManager();
        CacheStorage cacheStorage = serviceManager.getCacheStorage();
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
        config.setDiskBufferingConfiguration(
                DiskBufferingConfiguration.builder()
                        .setEnabled(true)
                        .setExportScheduleHandler(scheduleHandler)
                        .build());

        OpenTelemetryRum.builder(application, config).build(serviceManager);

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
                .build(createServiceManager());
        assertThat(seen.get()).isNotNull();
    }

    @Test
    public void diskBufferingDisabled() {
        ArgumentCaptor<SpanExporter> exporterCaptor = ArgumentCaptor.forClass(SpanExporter.class);
        ExportScheduleHandler scheduleHandler = mock();

        OtelRumConfig config = buildConfig();
        config.setDiskBufferingConfiguration(
                DiskBufferingConfiguration.builder()
                        .setEnabled(false)
                        .setExportScheduleHandler(scheduleHandler)
                        .build());

        OpenTelemetryRum.builder(application, config).build();

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
        OtelRumConfig otelRumConfig = buildConfig();
        otelRumConfig.setGlobalAttributes(
                () -> Attributes.of(stringKey("someGlobalKey"), "someGlobalValue"));

        OpenTelemetryRum rum =
                OpenTelemetryRum.builder(application, otelRumConfig)
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
                                .put("someGlobalKey", "someGlobalValue")
                                .put("localAttrKey", "localAttrValue")
                                .build());
    }

    @Test
    public void verifyServicesAreInitialized() {
        makeBuilder().build();

        assertThat(ServiceManager.get()).isNotNull();
    }

    @Test
    public void verifyServicesAreStarted() {
        ServiceManager serviceManager = mock();
        doReturn(mock(AppLifecycleService.class)).when(serviceManager).getAppLifecycleService();

        makeBuilder().build(serviceManager);

        verify(serviceManager).start();
    }

    @Test
    public void verifyPreconfiguredServicesInitialization() {
        OpenTelemetrySdk openTelemetrySdk = mock();
        // Work around sdk EventLogger api limitations
        LoggerProvider logsBridge = mock(LoggerProvider.class);
        LoggerBuilder loggerBuilder = mock();
        when(openTelemetrySdk.getLogsBridge()).thenReturn(logsBridge);
        when(logsBridge.loggerBuilder(any())).thenReturn(loggerBuilder);

        OpenTelemetryRum.builder(application, openTelemetrySdk, true).build();

        assertThat(ServiceManager.get()).isNotNull();
    }

    /**
     * @noinspection KotlinInternalInJava
     */
    private static ServiceManager createServiceManager() {
        return new ServiceManagerImpl(
                Arrays.asList(
                        mock(Preferences.class),
                        mock(CacheStorage.class),
                        mock(AppLifecycleService.class),
                        mock(VisibleScreenService.class)));
    }

    @NonNull
    private OpenTelemetryRumBuilder makeBuilder() {
        return OpenTelemetryRum.builder(application, buildConfig());
    }

    private OtelRumConfig buildConfig() {
        return new OtelRumConfig().disableNetworkAttributes().disableSdkInitializationEvents();
    }
}
