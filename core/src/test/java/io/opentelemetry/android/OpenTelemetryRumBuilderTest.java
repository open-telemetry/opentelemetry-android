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
                         equalTo(SessionIncubatingAttributes.SESSION_ID, sessionId),
                         equalTo(SCREEN_NAME_KEY, "unknown"));
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
         TextMapGetter<? super Object
 