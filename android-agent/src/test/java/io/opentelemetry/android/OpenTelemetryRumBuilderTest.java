/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY;
import static io.opentelemetry.android.common.RumConstants.SESSION_ID_KEY;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.os.Looper;
import androidx.annotation.NonNull;
import io.opentelemetry.android.config.OtelRumConfig;
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfiguration;
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter;
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler;
import io.opentelemetry.android.instrumentation.common.ApplicationStateListener;
import io.opentelemetry.android.instrumentation.startup.InitializationEvents;
import io.opentelemetry.android.internal.services.CacheStorageService;
import io.opentelemetry.android.internal.services.PreferencesService;
import io.opentelemetry.android.internal.services.Service;
import io.opentelemetry.android.internal.services.ServiceManager;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.disk.buffering.SpanToDiskExporter;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenTelemetryRumBuilderTest {

    final Resource resource =
            Resource.getDefault().toBuilder().put("test.attribute", "abcdef").build();
    final InMemorySpanExporter spanExporter = InMemorySpanExporter.create();
    final InMemoryLogRecordExporter logRecordExporter = InMemoryLogRecordExporter.create();

    @Mock Application application;
    @Mock Looper looper;
    @Mock android.content.Context applicationContext;
    @Mock Activity activity;
    @Mock ApplicationStateListener listener;

    @Mock InitializationEvents initializationEvents;
    @Captor ArgumentCaptor<Application.ActivityLifecycleCallbacks> activityCallbacksCaptor;

    @BeforeEach
    void setup() {
        when(application.getApplicationContext()).thenReturn(applicationContext);
        when(application.getMainLooper()).thenReturn(looper);
        ServiceManager.resetForTest();
    }

    @AfterEach
    void tearDown() {
        SignalFromDiskExporter.resetForTesting();
    }

    @Test
    void shouldRegisterApplicationStateWatcher() {
        makeBuilder().build();

        verify(application).registerActivityLifecycleCallbacks(isA(ApplicationStateWatcher.class));
    }

    @Test
    void shouldBuildTracerProvider() {
        OpenTelemetryRum openTelemetryRum =
                makeBuilder()
                        .setResource(resource)
                        .addTracerProviderCustomizer(
                                (tracerProviderBuilder, app) ->
                                        tracerProviderBuilder.addSpanProcessor(
                                                SimpleSpanProcessor.create(spanExporter)))
                        .build();

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
                        equalTo(SESSION_ID_KEY, sessionId), equalTo(SCREEN_NAME_KEY, "unknown"));
    }

    @Test
    void shouldInstallInstrumentation() {
        OpenTelemetryRum.builder(application, buildConfig())
                .addInstrumentation(
                        instrumentedApplication -> {
                            assertThat(instrumentedApplication.getApplication())
                                    .isSameAs(application);
                            instrumentedApplication.registerApplicationStateListener(listener);
                        })
                .build();

        verify(application).registerActivityLifecycleCallbacks(activityCallbacksCaptor.capture());

        activityCallbacksCaptor.getValue().onActivityStarted(activity);
        verify(listener).onApplicationForegrounded();

        activityCallbacksCaptor.getValue().onActivityStopped(activity);
        verify(listener).onApplicationBackgrounded();
    }

    private OtelRumConfig buildConfig() {
        return new OtelRumConfig().disableNetworkAttributes();
    }

    @Test
    void canAddPropagator() {
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
    void canSetPropagator() {
        TextMapPropagator customPropagator = mock(TextMapPropagator.class);

        OpenTelemetryRum rum = makeBuilder().addPropagatorCustomizer(x -> customPropagator).build();
        TextMapPropagator result = rum.getOpenTelemetry().getPropagators().getTextMapPropagator();
        assertThat(result).isSameAs(customPropagator);
    }

    @Test
    void setSpanExporterCustomizer() {
        SpanExporter exporter = mock(SpanExporter.class);
        Function<SpanExporter, SpanExporter> customizer = x -> exporter;
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
    }

    @Test
    void diskBufferingEnabled() {
        PreferencesService preferences = mock();
        CacheStorageService cacheStorage = mock();
        doReturn(60 * 1024 * 1024L).when(cacheStorage).ensureCacheSpaceAvailable(anyLong());
        setUpServiceManager(preferences, cacheStorage);
        OtelRumConfig config = buildConfig();
        ExportScheduleHandler scheduleHandler = mock();
        config.setDiskBufferingConfiguration(
                DiskBufferingConfiguration.builder()
                        .setEnabled(true)
                        .setExportScheduleHandler(scheduleHandler)
                        .build());
        ArgumentCaptor<SpanExporter> exporterCaptor = ArgumentCaptor.forClass(SpanExporter.class);

        OpenTelemetryRum.builder(application, config)
                .setInitializationEvents(initializationEvents)
                .build();

        assertThat(SignalFromDiskExporter.get()).isNotNull();
        verify(scheduleHandler).enable();
        verify(scheduleHandler, never()).disable();
        verify(initializationEvents).spanExporterInitialized(exporterCaptor.capture());
        assertThat(exporterCaptor.getValue()).isInstanceOf(SpanToDiskExporter.class);
    }

    @Test
    void diskBufferingEnabled_when_exception_thrown() {
        PreferencesService preferences = mock();
        CacheStorageService cacheStorage = mock();
        ExportScheduleHandler scheduleHandler = mock();
        doReturn(60 * 1024 * 1024L).when(cacheStorage).ensureCacheSpaceAvailable(anyLong());
        doAnswer(
                        invocation -> {
                            throw new IOException();
                        })
                .when(cacheStorage)
                .getCacheDir();
        setUpServiceManager(preferences, cacheStorage);
        ArgumentCaptor<SpanExporter> exporterCaptor = ArgumentCaptor.forClass(SpanExporter.class);
        OtelRumConfig config = buildConfig();
        config.setDiskBufferingConfiguration(
                DiskBufferingConfiguration.builder()
                        .setEnabled(true)
                        .setExportScheduleHandler(scheduleHandler)
                        .build());

        OpenTelemetryRum.builder(application, config)
                .setInitializationEvents(initializationEvents)
                .build();

        verify(initializationEvents).spanExporterInitialized(exporterCaptor.capture());
        verify(scheduleHandler, never()).enable();
        verify(scheduleHandler).disable();
        assertThat(exporterCaptor.getValue()).isNotInstanceOf(SpanToDiskExporter.class);
        assertThat(SignalFromDiskExporter.get()).isNull();
    }

    @Test
    void diskBufferingDisabled() {
        ArgumentCaptor<SpanExporter> exporterCaptor = ArgumentCaptor.forClass(SpanExporter.class);
        ExportScheduleHandler scheduleHandler = mock();

        OtelRumConfig config = buildConfig();
        config.setDiskBufferingConfiguration(
                DiskBufferingConfiguration.builder()
                        .setEnabled(false)
                        .setExportScheduleHandler(scheduleHandler)
                        .build());

        OpenTelemetryRum.builder(application, config)
                .setInitializationEvents(initializationEvents)
                .build();

        verify(initializationEvents).spanExporterInitialized(exporterCaptor.capture());
        verify(scheduleHandler, never()).enable();
        verify(scheduleHandler).disable();
        assertThat(exporterCaptor.getValue()).isNotInstanceOf(SpanToDiskExporter.class);
        assertThat(SignalFromDiskExporter.get()).isNull();
    }

    @Test
    void verifyGlobalAttrsForLogs() {
        OtelRumConfig otelRumConfig = buildConfig();
        otelRumConfig.setGlobalAttributes(
                () -> Attributes.of(AttributeKey.stringKey("someGlobalKey"), "someGlobalValue"));

        OpenTelemetryRum rum =
                OpenTelemetryRum.builder(application, otelRumConfig)
                        .addLoggerProviderCustomizer(
                                (sdkLoggerProviderBuilder, application) ->
                                        sdkLoggerProviderBuilder.addLogRecordProcessor(
                                                SimpleLogRecordProcessor.create(logRecordExporter)))
                        .build();

        Logger logger = rum.getOpenTelemetry().getLogsBridge().loggerBuilder("LogScope").build();
        logger.logRecordBuilder()
                .setAttribute(AttributeKey.stringKey("localAttrKey"), "localAttrValue")
                .emit();

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
    void verifyServicesAreInitialized() {
        makeBuilder().build();

        assertThat(ServiceManager.get()).isNotNull();
    }

    @Test
    void verifyServicesAreStarted() {
        setUpServiceManager();

        makeBuilder().build();

        verify(ServiceManager.get()).start();
    }

    private static void setUpServiceManager(Service... services) {
        ServiceManager serviceManager = mock();
        for (Service service : services) {
            doReturn(service).when(serviceManager).getService(service.getClass());
        }
        ServiceManager.setForTest(serviceManager);
    }

    @NonNull
    private OpenTelemetryRumBuilder makeBuilder() {
        return OpenTelemetryRum.builder(application, buildConfig());
    }
}
