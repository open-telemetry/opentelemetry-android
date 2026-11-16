Warning: truncated output (original token count: 8484)
Total output lines: 896

/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter.Companion.resetForTesting
import io.opentelemetry.android.features.diskbuffering.scheduler.ScheduleEnablement
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoaderImpl
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.Services.Companion.set
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.network.CurrentNetworkProvider
import io.opentelemetry.android.internal.services.network.NetworkProviderHolder
import io.opentelemetry.android.internal.services.storage.CacheStorage
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.contrib.disk.buffering.exporters.SpanToDiskExporter
import io.opentelemetry.kotlin.semconv.AppAttributes.APP_SCREEN_NAME
import io.opentelemetry.kotlin.semconv.IncubatingApi
import io.opentelemetry.kotlin.semconv.SessionAttributes.SESSION_ID
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.testing.assertj.LongPointAssert
import io.opentelemetry.sdk.testing.assertj.LongSumAssert
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions
import io.opentelemetry.sdk.testing.assertj.ResourceAssert
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

@OptIn(IncubatingApi::class)
@RunWith(AndroidJUnit4::class)
@Suppress("LargeClass")
class OpenTelemetryRumBuilderTest {
    private val resource: Resource =
        Resource
            .getDefault()
            .toBuilder()
            .put("test.attribute", "abcdef")
            .build()
    private val spanExporter: InMemorySpanExporter = InMemorySpanExporter.create()
    private val logsExporter: InMemoryLogRecordExporter = InMemoryLogRecordExporter.create()

    @MockK
    private lateinit var application: Application

    @MockK
    private lateinit var looper: Looper

    @MockK
    private lateinit var applicationContext: Context

    @MockK
    private lateinit var initializationEvents: InitializationEvents

    @MockK
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        every { application.applicationContext } returns applicationContext
        every { application.cacheDir } returns File(System.getProperty("java.io.tmpdir") ?: "/tmp")
        every { application.mainLooper } returns looper
        every { application.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        InitializationEvents.set(initializationEvents)
        NetworkProviderHolder.set(mockk<CurrentNetworkProvider>(relaxed = true))
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        clearAllMocks()
        resetForTesting()
        InitializationEvents.resetForTest()
        set(null)
        NetworkProviderHolder.set(null)
    }

    @Test
    fun shouldBuildTracerProvider() {
        createAndSetServiceManager()
        val openTelemetryRum =
            makeBuilder()
                .setResource(resource)
                .addTracerProviderCustomizer { tracerProviderBuilder: SdkTracerProviderBuilder, _: Context ->
                    tracerProviderBuilder.addSpanProcessor(
                        SimpleSpanProcessor.create(spanExporter),
                    )
                }.build()

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val sessionId = openTelemetryRum.sessionProvider.getSessionId()
                openTelemetryRum
                    .openTelemetry
                    .getTracer("test")
                    .spanBuilder("test span")
                    .startSpan()
                    .end()

                val spans = spanExporter.finishedSpanItems
                assertThat(spans).hasSize(1)
                OpenTelemetryAssertions
                    .assertThat(spans[0])
                    .hasName("test span")
                    .hasResource(resource)
                    .hasAttributesSatisfyingExactly(
                        OpenTelemetryAssertions.equalTo(
                            stringKey(SESSION_ID),
                            sessionId,
                        ),
                        OpenTelemetryAssertions.equalTo(
                            stringKey(APP_SCREEN_NAME),
                            CUR_SCREEN_NAME,
                        ),
                    )
            }
    }

    @Test
    fun shouldBuildLogRecordProvider() {
        createAndSetServiceManager()
        val openTelemetryRum =
            makeBuilder()
                .setResource(resource)
                .addLoggerProviderCustomizer { logRecordProviderBuilder: SdkLoggerProviderBuilder, _: Context ->
                    logRecordProviderBuilder.addLogRecordProcessor(
                        SimpleLogRecordProcessor.create(logsExporter),
                    )
                }.build()

        val attrs =
            Attributes.of(
                AttributeKey.stringKey("mega"),
                "hit",
                AttributeKey.stringKey("body.field"),
                "foo",
            )
        openTelemetryRum.emitEvent("test.event", "", attrs)

        val logs = logsExporter.finishedLogRecordItems
        assertThat(logs).hasSize(1)
        OpenTelemetryAssertions
            .assertThat(logs[0])
            .hasAttributesSatisfyingExactly(
                OpenTelemetryAssertions.equalTo(
                    stringKey(SESSION_ID),
                    openTelemetryRum.sessionProvider.getSessionId(),
                ),
                OpenTelemetryAssertions.equalTo(stringKey(APP_SCREEN_NAME), CUR_SCREEN_NAME),
                OpenTelemetryAssertions.equalTo(AttributeKey.stringKey("mega"), "hit"),
                OpenTelemetryAssertions.equalTo(
                    AttributeKey.stringKey("body.field"),
                    "foo",
                ),
            ).hasResource(resource)
            .hasEventName("test.event")
    }

    @Test
    fun preservesExplicitSessionIdOnLogs() {
        createAndSetServiceManager()
        val openTelemetryRum =
            makeBuilder()
                .setSessionProvider(SessionProvider { "current-session" })
                .addLoggerProviderCustomizer { logRecordProviderBuilder: SdkLoggerProviderBuilder, _: Context ->
                    logRecordProviderBuilder.addLogRecordProcessor(
                        SimpleLogRecordProcessor.create(logsExporter),
                    )
                }.build()

        openTelemetryRum.openTelemetry.logsBridge
            .loggerBuilder("test")
            .build()
            .logRecordBuilder()
            .setAttribute(stringKey(SESSION_ID), "persisted-session")
            .emit()

        assertThat(
            logsExporter.finishedLogRecordItems
                .single()
                .attributes
                .get(stringKey(SESSION_ID)),
        ).isEqualTo("persisted-session")
    }

    @Test
    fun canCustomizeMetrics() {
        val metricReader = InMemoryMetricReader.create()
        val openTelemetryRum =
            makeBuilder()
  …3484 tokens truncated…)
        config.setDiskBufferingConfig(DiskBufferingConfig(true))
        config.setLoggingDisabled()
        config.setMetricsDisabled()

        RumBuilder
            .builder(application, config)
            .setExportScheduleEnablement(scheduleHandler)
            .addLogRecordExporterCustomizer {
                logExporterCustomizerCalled = true
                it
            }.addMetricExporterCustomizer {
                metricExporterCustomizerCalled = true
                it
            }.build()

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                verify(exactly = 1) { scheduleHandler.enable() }
                assertThat(logExporterCustomizerCalled).isFalse()
                assertThat(metricExporterCustomizerCalled).isFalse()
            }
    }

    @Test
    fun diskBufferingDisabledScheduleWhenAllSignalsDisabled() {
        createAndSetServiceManager()
        val scheduleHandler = mockk<ScheduleEnablement>(relaxed = true)
        val events = mockk<InitializationEvents>(relaxed = true)
        InitializationEvents.resetForTest()
        InitializationEvents.set(events)
        val cacheStorage = mockk<CacheStorage>()
        every { cacheStorage.cacheDir } answers { throw IOException() }

        val config = buildConfig().enableSdkInitializationEvents()
        config.setDiskBufferingConfig(DiskBufferingConfig(true))
        config.setTracingDisabled()
        config.setLoggingDisabled()
        config.setMetricsDisabled()

        RumBuilder
            .builder(application, config)
            .setExportScheduleEnablement(scheduleHandler)
            .setCacheStorage(cacheStorage)
            .build()

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                verify(exactly = 0) { events.spanExporterInitialized(any()) }
                verify(exactly = 0) { scheduleHandler.enable() }
                verify(atLeast = 1) { scheduleHandler.disable() }
                verify { cacheStorage wasNot Called }
            }
    }

    @Test
    fun otelReadyListeners() {
        val config = buildConfig()
        val seen = AtomicReference<OpenTelemetry>()
        createAndSetServiceManager()
        RumBuilder
            .builder(application, config)
            .addOtelReadyListener { otel: OpenTelemetry -> seen.set(otel) }
            .build()
        assertThat(seen.get()).isNotNull()
    }

    @Test
    fun throwingOtelReadyListenerDoesNotStopOtherListeners() {
        val config = buildConfig()
        val seen = AtomicReference<OpenTelemetry>()
        createAndSetServiceManager()
        val rum =
            RumBuilder
                .builder(application, config)
                .addOtelReadyListener { error("listener failed") }
                .addOtelReadyListener { otel: OpenTelemetry -> seen.set(otel) }
                .build()
        assertThat(rum).isNotNull()
        assertThat(seen.get()).isNotNull()
    }

    @Test
    fun diskBufferingDisabled() {
        val exporterSlot = slot<SpanExporter>()
        val scheduleHandler = mockk<ScheduleEnablement>(relaxed = true)
        val events = mockk<InitializationEvents>(relaxed = true)
        InitializationEvents.resetForTest()
        InitializationEvents.set(events)

        val config = buildConfig().enableSdkInitializationEvents()
        config.setDiskBufferingConfig(DiskBufferingConfig())

        RumBuilder
            .builder(application, config)
            .setExportScheduleEnablement(scheduleHandler)
            .build()

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                verify(exactly = 1) { events.spanExporterInitialized(capture(exporterSlot)) }
                verify(exactly = 0) { scheduleHandler.enable() }
                verify(exactly = 1) { scheduleHandler.disable() }

                assertThat(exporterSlot.captured)
                    .isNotInstanceOf(SpanToDiskExporter::class.java)
                assertThat(SignalFromDiskExporter.get())
                    .isNull()
            }
    }

    @Test
    fun verifyGlobalAttrsForLogs() {
        createAndSetServiceManager()
        val otelRumConfig = buildConfig()
        otelRumConfig.setGlobalAttributes {
            Attributes.of(
                stringKey("someGlobalKey"),
                "someGlobalValue",
            )
        }

        val rum =
            RumBuilder
                .builder(application, otelRumConfig)
                .addLoggerProviderCustomizer { sdkLoggerProviderBuilder: SdkLoggerProviderBuilder, _: Context ->
                    sdkLoggerProviderBuilder.addLogRecordProcessor(
                        SimpleLogRecordProcessor.create(logsExporter),
                    )
                }.build()

        val logger =
            rum
                .openTelemetry
                .logsBridge
                .loggerBuilder("LogScope")
                .build()
        logger
            .logRecordBuilder()
            .setAttribute(AttributeKey.stringKey("localAttrKey"), "localAttrValue")
            .emit()

        val recordedLogs = logsExporter.finishedLogRecordItems
        assertThat(recordedLogs).hasSize(1)
        val logRecordData: LogRecordData = recordedLogs[0]
        OpenTelemetryAssertions
            .assertThat(logRecordData)
            .hasAttributes(
                Attributes
                    .builder()
                    .put(SESSION_ID, rum.sessionProvider.getSessionId())
                    .put("someGlobalKey", "someGlobalValue")
                    .put("localAttrKey", "localAttrValue")
                    .put(stringKey(APP_SCREEN_NAME), CUR_SCREEN_NAME)
                    .build(),
            )
    }

    @Test
    fun initializationEventsDisabledByDefault() {
        createAndSetServiceManager()
        val events = mockk<InitializationEvents>(relaxed = true)
        InitializationEvents.resetForTest()
        InitializationEvents.set(events)

        RumBuilder
            .builder(application, buildConfig())
            .build()

        verify { events wasNot Called }
    }

    @OptIn(IncubatingApi::class)
    @Test
    fun disableSessionIdPreventsSessionIdOnSpansAndLogs() {
        createAndSetServiceManager()
        val openTelemetryRum =
            RumBuilder
                .builder(
                    application,
                    buildConfig().disableSessionId(),
                ).setSessionProvider(SessionProvider { "test-session" })
                .addTracerProviderCustomizer { tracerProviderBuilder: SdkTracerProviderBuilder, _: Context ->
                    tracerProviderBuilder.addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                }.addLoggerProviderCustomizer { logRecordProviderBuilder: SdkLoggerProviderBuilder, _: Context ->
                    logRecordProviderBuilder.addLogRecordProcessor(
                        SimpleLogRecordProcessor.create(logsExporter),
                    )
                }.build()

        openTelemetryRum.openTelemetry
            .getTracer("test")
            .spanBuilder("test-span")
            .startSpan()
            .end()
        openTelemetryRum.openTelemetry.logsBridge
            .loggerBuilder("test")
            .build()
            .logRecordBuilder()
            .emit()

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.attributes.get(stringKey(SESSION_ID))).isNull()

        val log = logsExporter.finishedLogRecordItems.single()
        assertThat(log.attributes.get(stringKey(SESSION_ID))).isNull()
    }

    @OptIn(IncubatingApi::class)
    @Test
    fun disableScreenAttributesPreventsScreenNameOnSpansAndLogs() {
        createAndSetServiceManager()
        val openTelemetryRum =
            RumBuilder
                .builder(
                    application,
                    buildConfig().disableScreenAttributes(),
                ).addTracerProviderCustomizer { tracerProviderBuilder: SdkTracerProviderBuilder, _: Context ->
                    tracerProviderBuilder.addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                }.addLoggerProviderCustomizer { logRecordProviderBuilder: SdkLoggerProviderBuilder, _: Context ->
                    logRecordProviderBuilder.addLogRecordProcessor(
                        SimpleLogRecordProcessor.create(logsExporter),
                    )
                }.build()

        openTelemetryRum.openTelemetry
            .getTracer("test")
            .spanBuilder("test-span")
            .startSpan()
            .end()
        openTelemetryRum.openTelemetry.logsBridge
            .loggerBuilder("test")
            .build()
            .logRecordBuilder()
            .emit()

        val span = spanExporter.finishedSpanItems.single()
        assertThat(span.attributes.get(stringKey(APP_SCREEN_NAME))).isNull()

        val log = logsExporter.finishedLogRecordItems.single()
        assertThat(log.attributes.get(stringKey(APP_SCREEN_NAME))).isNull()
    }

    private fun makeBuilder(): OpenTelemetryRumBuilder = RumBuilder.builder(application, buildConfig())

    private fun buildConfig(): OtelRumConfig =
        OtelRumConfig()
            .disableNetworkAttributes()
            .disableSdkInitializationEvents()
            .suppressInstrumentation("network")

    companion object {
        const val CUR_SCREEN_NAME: String = "Celebratory Token"

        private fun createAndSetServiceManager(): Services {
            val services = mockk<Services>()
            every { services.appLifecycle } returns mockk<AppLifecycle>()
            val screenService = mockk<VisibleScreenTracker>()
            every { screenService.currentlyVisibleScreen } returns CUR_SCREEN_NAME
            every { services.visibleScreenTracker } returns screenService
            set(services)
            return services
        }
    }
}
