/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import android.app.*;
import android.net.*;
import android.os.*;
import androidx.annotation.*;
import androidx.test.ext.junit.runners.*;
import static io.opentelemetry.android.common.RumConstants.*;
import io.opentelemetry.android.config.*;
import io.opentelemetry.android.features.diskbuffering.*;
import io.opentelemetry.android.features.diskbuffering.scheduler.*;
import io.opentelemetry.android.instrumentation.*;
import io.opentelemetry.android.instrumentation.internal.*;
import io.opentelemetry.android.internal.initialization.*;
import io.opentelemetry.android.internal.services.*;
import io.opentelemetry.android.internal.services.applifecycle.*;
import io.opentelemetry.android.internal.services.visiblescreen.*;
import io.opentelemetry.android.internal.session.*;
import io.opentelemetry.android.session.*;
import static io.opentelemetry.api.common.AttributeKey.*;
import io.opentelemetry.api.common.*;
import io.opentelemetry.api.incubator.events.*;
import io.opentelemetry.api.logs.*;
import io.opentelemetry.api.metrics.*;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.*;
import io.opentelemetry.context.propagation.*;
import io.opentelemetry.contrib.disk.buffering.*;
import io.opentelemetry.sdk.*;
import io.opentelemetry.sdk.logs.data.*;
import io.opentelemetry.sdk.logs.export.*;
import io.opentelemetry.sdk.logs.internal.*;
import io.opentelemetry.sdk.metrics.*;
import io.opentelemetry.sdk.metrics.data.*;
import io.opentelemetry.sdk.metrics.export.*;
import io.opentelemetry.sdk.resources.*;
import io.opentelemetry.sdk.testing.assertj.*;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.*;
import io.opentelemetry.sdk.testing.exporter.*;
import io.opentelemetry.sdk.trace.data.*;
import io.opentelemetry.sdk.trace.export.*;
import static io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import static org.awaitility.Awaitility.*;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(AndroidJUnit4.class)
public class OpenTelemetryRumBuilderTest {

    public static final String CUR_SCREEN_NAME = "Celebratory Token";
    final Resource resource =
            Resource.getDefault().toBuilder().put("test.attribute", "abcdef").build();
    final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
    final InMemoryLogRecordExporter logsExporter = InMemoryLogRecordExporter.create();

    @Mock Application application;
    @Mock Looper looper;
    @Mock android.content.Context applicationContext;

    @Mock InitializationEvents initializationEvents;
    @Mock ConnectivityManager connectivityManager;
    private AutoCloseable mocks;

    @Before
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        when(application.getApplicationContext()).thenReturn(applicationContext);
        when(application.getMainLooper()).thenReturn(looper);
        when(application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE))
                .thenReturn(connectivityManager);
        InitializationEvents.set(initializationEvents);
    }

    @After
    public void tearDown() throws Exception {
        SignalFromDiskExporter.resetForTesting();
        InitializationEvents.resetForTest();
        AndroidInstrumentationLoader.resetForTest();
        mocks.close();
        Services.set(null);
    }

    @Test
    public void shouldRegisterApplicationStateWatcher() {
        Services services = createAndSetServiceManager();
        AppLifecycle appLifecycle = services.getAppLifecycle();

        makeBuilder().build();

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
        createAndSetServiceManager();
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
    public void canCustomizeMetrics() {
        InMemoryMetricReader metricReader = InMemoryMetricReader.create();
        OpenTelemetryRum openTelemetryRum =
                makeBuilder()
                        .setResource(resource)
                        .addMeterProviderCustomizer(
                                (sdkMeterProviderBuilder, application) -> {
                                    Attributes metricResAttrs =
                                            Attributes.of(stringKey("mmm"), "nnn");
                                    return sdkMeterProviderBuilder
                                            .setResource(Resource.create(metricResAttrs))
                                            .registerMetricReader(metricReader);
                                })
                        .build();

        OpenTelemetrySdk sdk = (OpenTelemetrySdk) openTelemetryRum.getOpenTelemetry();
        Meter meter = sdk.getSdkMeterProvider().meterBuilder("myMeter").build();
        Attributes counterAttrs = Attributes.of(longKey("adams"), 42L);
        LongCounter counter = meter.counterBuilder("myCounter").build();
        counter.add(40, counterAttrs);
        metricReader.forceFlush();
        counter.add(2, counterAttrs);

        List<MetricData> metrics = new ArrayList<>(metricReader.collectAllMetrics());
        assertThat(metrics).hasSize(1);
        assertThat(metrics.get(0))
                .hasName("myCounter")
                .hasLongSumSatisfying(
                        sum ->
                                sum.hasPointsSatisfying(
                                        pt -> pt.hasValue(42L).hasAttributes(counterAttrs)))
                .hasResourceSatisfying(res -> res.hasAttribute(stringKey("mmm"), "nnn"));
    }

    @Test
    public void canCustomizeMetricExport() {
        InMemoryMetricExporter exporter =
                InMemoryMetricExporter.create(AggregationTemporality.DELTA); // NOT THE DEFAULT
        PeriodicMetricReader periodicReader = PeriodicMetricReader.builder(exporter).build();
        OpenTelemetryRum openTelemetryRum =
                makeBuilder()
                        .setResource(resource)
                        .addMeterProviderCustomizer(
                                (builder, app) ->
                                        SdkMeterProvider.builder()
                                                .registerMetricReader(periodicReader))
                        .addMetricExporterCustomizer(x -> exporter)
                        .build();

        OpenTelemetrySdk sdk = (OpenTelemetrySdk) openTelemetryRum.getOpenTelemetry();
        Meter meter = sdk.getSdkMeterProvider().meterBuilder("FOOMETER").build();
        LongCounter counter = meter.counterBuilder("FOOCOUNTER").build();
        counter.add(22);
        periodicReader.forceFlush();
        counter.add(2);
        counter.add(3);
        periodicReader.forceFlush();
        List<MetricData> metrics = exporter.getFinishedMetricItems();

        assertThat(metrics).hasSize(2);
        assertThat(metrics.get(0))
                .hasName("FOOCOUNTER")
                .hasLongSumSatisfying(sum -> sum.hasPointsSatisfying(pt -> pt.hasValue(22L)));
        assertThat(metrics.get(1))
                .hasName("FOOCOUNTER")
                .hasLongSumSatisfying(sum -> sum.hasPointsSatisfying(pt -> pt.hasValue(5L)));
    }

    @Test
    public void shouldInstallInstrumentation() {
        Services services = createAndSetServiceManager();
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
                        .setSessionManager(sessionManager)
                        .build();

        verify(services.getAppLifecycle()).registerListener(timeoutHandler);

        InstallationContext expectedCtx =
                new InstallationContext(application, rum.getOpenTelemetry(), sessionManager);
        verify(localInstrumentation).install(eq(expectedCtx));
        verify(classpathInstrumentation).install(eq(expectedCtx));
    }

    @Test
    public void shouldInstallInstrumentation_excludingClasspathImplsWhenRequestedInConfig() {
        Services services = createAndSetServiceManager();
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
                        .setSessionManager(sessionManager)
                        .build();

        verify(services.getAppLifecycle()).registerListener(timeoutHandler);

        InstallationContext expectedCtx =
                new InstallationContext(application, rum.getOpenTelemetry(), sessionManager);
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
        createAndSetServiceManager();
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
                .hasAttributesSatisfyingExactly(
                        equalTo(stringKey("bing"), "bang"),
                        equalTo(SCREEN_NAME_KEY, CUR_SCREEN_NAME),
                        equalTo(SESSION_ID, rum.getRumSessionId()))
                .hasSeverity(Severity.FATAL3);
    }

    @Test
    public void diskBufferingEnabled() {
        createAndSetServiceManager();
        OtelRumConfig config = buildConfig();
        ExportScheduleHandler scheduleHandler = mock();
        config.setDiskBufferingConfig(new DiskBufferingConfig(true));
        ArgumentCaptor<SpanExporter> exporterCaptor = ArgumentCaptor.forClass(SpanExporter.class);

        OpenTelemetryRum.builder(application, config)
                .setExportScheduleHandler(scheduleHandler)
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
        Services services = createAndSetServiceManager();
        CacheStorage cacheStorage = services.getCacheStorage();
        ExportScheduleHandler scheduleHandler = mock();
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
        createAndSetServiceManager();
        OpenTelemetryRum.builder(application, config).addOtelSdkReadyListener(seen::set).build();
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
        createAndSetServiceManager();
        OtelRumConfig otelRumConfig = buildConfig();
        otelRumConfig.setGlobalAttributes(
                () -> Attributes.of(stringKey("someGlobalKey"), "someGlobalValue"));

        OpenTelemetryRum rum =
                OpenTelemetryRum.builder(application, otelRumConfig)
                        .addLoggerProviderCustomizer(
                                (sdkLoggerProviderBuilder, application) ->
                                        sdkLoggerProviderBuilder.addLogRecordProcessor(
                                                SimpleLogRecordProcessor.create(logsExporter)))
                        .build();

        Logger logger = rum.getOpenTelemetry().getLogsBridge().loggerBuilder("LogScope").build();
        logger.logRecordBuilder().setAttribute(stringKey("localAttrKey"), "localAttrValue").emit();

        List<LogRecordData> recordedLogs = logsExporter.getFinishedLogRecordItems();
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

    /**
     * @noinspection KotlinInternalInJava
     */
    private static Services createAndSetServiceManager() {
        Services services = mock(Services.class);
        when(services.getAppLifecycle()).thenReturn(mock(AppLifecycle.class));
        when(services.getCacheStorage()).thenReturn(mock(CacheStorage.class));
        when(services.getPreferences()).thenReturn(mock(Preferences.class));
        VisibleScreenTracker screenService = mock(VisibleScreenTracker.class);
        when(screenService.getCurrentlyVisibleScreen()).thenReturn(CUR_SCREEN_NAME);
        when(services.getVisibleScreenTracker()).thenReturn(screenService);
        Services.set(services);
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
