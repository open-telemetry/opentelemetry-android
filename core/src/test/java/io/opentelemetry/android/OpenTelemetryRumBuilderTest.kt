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
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter.Companion.resetForTesting
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.internal.AndroidInstrumentationLoaderImpl
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.Services.Companion.set
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.storage.CacheStorage
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.contrib.disk.buffering.exporters.SpanToDiskExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
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
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes
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

@RunWith(AndroidJUnit4::class)
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
        every { application.mainLooper } returns looper
        every { application.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
        InitializationEvents.set(initializationEvents)
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
        clearAllMocks()
        resetForTesting()
        InitializationEvents.resetForTest()
        AndroidInstrumentationLoader.resetForTest()
        set(null)
    }

    @Test
    fun shouldBuildTracerProvider() {
        createAndSetServiceManager()
        val openTelemetryRum =
            makeBuilder()
                .setResource(resource)
                .addTracerProviderCustomizer { tracerProviderBuilder: SdkTracerProviderBuilder, app: Context ->
                    tracerProviderBuilder.addSpanProcessor(
                        SimpleSpanProcessor.create(spanExporter),
                    )
                }.build()

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                val sessionId = openTelemetryRum.getRumSessionId()
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
                            SessionIncubatingAttributes.SESSION_ID,
                            sessionId,
                        ),
                        OpenTelemetryAssertions.equalTo(
                            SCREEN_NAME_KEY,
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
                .addLoggerProviderCustomizer { logRecordProviderBuilder: SdkLoggerProviderBuilder, app: Context ->
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
                    SessionIncubatingAttributes.SESSION_ID,
                    openTelemetryRum.getRumSessionId(),
                ),
                OpenTelemetryAssertions.equalTo(SCREEN_NAME_KEY, CUR_SCREEN_NAME),
                OpenTelemetryAssertions.equalTo(AttributeKey.stringKey("mega"), "hit"),
                OpenTelemetryAssertions.equalTo(
                    AttributeKey.stringKey("body.field"),
                    "foo",
                ),
            ).hasResource(resource)
        // TODO: verify event name inline above when the assertions framework can handle it
        val log0 = logs[0]
        assertThat(log0.eventName).isEqualTo("test.event")
    }

    @Test
    fun canCustomizeMetrics() {
        val metricReader = InMemoryMetricReader.create()
        val openTelemetryRum =
            makeBuilder()
                .setResource(resource)
                .addMeterProviderCustomizer { sdkMeterProviderBuilder: SdkMeterProviderBuilder, application: Context ->
                    val metricResAttrs =
                        Attributes.of(AttributeKey.stringKey("mmm"), "nnn")
                    sdkMeterProviderBuilder
                        .setResource(Resource.create(metricResAttrs))
                        .registerMetricReader(metricReader)
                }.build()

        val sdk = openTelemetryRum.openTelemetry as OpenTelemetrySdk
        val meter = sdk.sdkMeterProvider.meterBuilder("myMeter").build()
        val counterAttrs = Attributes.of(AttributeKey.longKey("adams"), 42L)
        val counter = meter.counterBuilder("myCounter").build()
        counter.add(40, counterAttrs)
        metricReader.forceFlush()
        counter.add(2, counterAttrs)

        val metrics: MutableList<MetricData> =
            ArrayList(metricReader.collectAllMetrics())
        assertThat(metrics).hasSize(1)
        OpenTelemetryAssertions
            .assertThat(metrics[0])
            .hasName("myCounter")
            .hasLongSumSatisfying { sum: LongSumAssert ->
                sum.hasPointsSatisfying({ pt: LongPointAssert ->
                    pt.hasValue(42L).hasAttributes(counterAttrs)
                })
            }.hasResourceSatisfying { res: ResourceAssert ->
                res.hasAttribute(
                    AttributeKey.stringKey("mmm"),
                    "nnn",
                )
            }
    }

    @Test
    fun canCustomizeMetricExport() {
        val exporter =
            InMemoryMetricExporter.create(AggregationTemporality.DELTA) // NOT THE DEFAULT
        val periodicReader = PeriodicMetricReader.builder(exporter).build()
        val openTelemetryRum =
            makeBuilder()
                .setResource(resource)
                .addMeterProviderCustomizer { builder: SdkMeterProviderBuilder, app: Context ->
                    SdkMeterProvider
                        .builder()
                        .registerMetricReader(periodicReader)
                }.addMetricExporterCustomizer(Function { x: MetricExporter -> exporter })
                .build()

        val sdk = openTelemetryRum.openTelemetry as OpenTelemetrySdk
        val meter = sdk.sdkMeterProvider.meterBuilder("FOOMETER").build()
        val counter = meter.counterBuilder("FOOCOUNTER").build()
        counter.add(22)
        periodicReader.forceFlush()
        counter.add(2)
        counter.add(3)
        periodicReader.forceFlush()
        val metrics = exporter.finishedMetricItems

        assertThat(metrics).hasSize(2)
        OpenTelemetryAssertions
            .assertThat(metrics[0])
            .hasName("FOOCOUNTER")
            .hasLongSumSatisfying { sum: LongSumAssert ->
                sum.hasPointsSatisfying({ pt: LongPointAssert -> pt.hasValue(22L) })
            }
        OpenTelemetryAssertions
            .assertThat(metrics[1])
            .hasName("FOOCOUNTER")
            .hasLongSumSatisfying { sum: LongSumAssert ->
                sum.hasPointsSatisfying({ pt: LongPointAssert -> pt.hasValue(5L) })
            }
    }

    @Test
    fun shouldInstallInstrumentation() {
        createAndSetServiceManager()
        val sessionProvider = mockk<SessionProvider>()
        val localInstrumentation =
            mockk<AndroidInstrumentation>(relaxed = true) {
                every { name } returns "local"
            }
        val classpathInstrumentation =
            mockk<AndroidInstrumentation>(relaxed = true) {
                every { name } returns "classpath"
            }

        val androidInstrumentationServices =
            AndroidInstrumentationLoader.get() as AndroidInstrumentationLoaderImpl
        androidInstrumentationServices.registerForTest(classpathInstrumentation)

        OpenTelemetryRumBuilder(application, buildConfig())
            .addInstrumentation(localInstrumentation)
            .setSessionProvider(sessionProvider)
            .build()

        verify(exactly = 1) { localInstrumentation.install(any()) }
        verify(exactly = 1) { classpathInstrumentation.install(any()) }
    }

    @Test
    fun shouldInstallInstrumentation_excludingClasspathImplsWhenRequestedInConfig() {
        createAndSetServiceManager()
        val sessionProvider = mockk<SessionProvider>()
        val localInstrumentation =
            mockk<AndroidInstrumentation>(relaxed = true) {
                every { name } returns "local"
            }
        val classpathInstrumentation =
            mockk<AndroidInstrumentation>(relaxed = true) {
                every { name } returns "classpath"
            }
        val androidInstrumentationServices =
            AndroidInstrumentationLoader.get() as AndroidInstrumentationLoaderImpl
        androidInstrumentationServices.registerForTest(classpathInstrumentation)

        OpenTelemetryRumBuilder(
            application,
            buildConfig().disableInstrumentationDiscovery(),
        ).addInstrumentation(localInstrumentation)
            .setSessionProvider(sessionProvider)
            .build()

        verify(exactly = 1) { localInstrumentation.install(any()) }
        verify(exactly = 0) { classpathInstrumentation.install(any()) }
    }

    @Test
    fun canAddPropagator() {
        val context =
            io.opentelemetry.context.Context
                .root()
        val carrier = Any()

        val expected = mockk<io.opentelemetry.context.Context>()
        val customPropagator = mockk<TextMapPropagator>()

        val getter: TextMapGetter<Any> = mockk()
        every {
            customPropagator.extract(
                context,
                carrier,
                getter,
            )
        } returns expected

        val rum =
            makeBuilder()
                .addPropagatorCustomizer(Function { x: TextMapPropagator -> customPropagator })
                .build()
        val result =
            rum
                .openTelemetry
                .propagators
                .textMapPropagator
                .extract(context, carrier, getter)
        assertThat(result).isSameAs(expected)
    }

    @Test
    fun canSetPropagator() {
        val customPropagator = mockk<TextMapPropagator>()

        val rum =
            makeBuilder()
                .addPropagatorCustomizer(Function { x: TextMapPropagator -> customPropagator })
                .build()
        val result = rum.openTelemetry.propagators.textMapPropagator
        assertThat(result).isSameAs(customPropagator)
    }

    @Test
    fun setSpanExporterCustomizer() {
        // Create a relaxed mock so un-stubbed calls are safe
        val exporter = mockk<SpanExporter>(relaxed = true)

        // Stub export() to return success
        every { exporter.export(any()) } returns CompletableResultCode.ofSuccess()

        val wasCalled = AtomicBoolean(false)
        val customizer: (SpanExporter) -> SpanExporter = {
            wasCalled.set(true)
            exporter
        }

        val rum =
            makeBuilder()
                .addSpanExporterCustomizer(customizer)
                .build()

        val span =
            rum
                .openTelemetry
                .getTracer("test")
                .spanBuilder("foo")
                .startSpan()

        try {
            span.makeCurrent().use {
                // no-op
            }
        } finally {
            span.end()
        }

        // Default is 5s, allow up to 30s for export to happen
        Awaitility
            .await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                verify { exporter.export(any()) }
            }

        assertThat(wasCalled.get()).isTrue()
    }

    @Test
    fun setLogRecordExporterCustomizer() {
        createAndSetServiceManager()
        val wasCalled = AtomicBoolean(false)
        val customizer: Function<LogRecordExporter, LogRecordExporter> =
            Function { x: LogRecordExporter ->
                wasCalled.set(true)
                logsExporter
            }
        val rum = makeBuilder().addLogRecordExporterCustomizer(customizer).build()

        val logger =
            rum
                .openTelemetry
                .logsBridge
                .loggerBuilder("LogScope")
                .build()
        logger
            .logRecordBuilder()
            .setBody("foo")
            .setSeverity(Severity.FATAL3)
            .setAttribute(AttributeKey.stringKey("bing"), "bang")
            .emit()
        // 5 sec is default
        Awaitility
            .await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                assertThat(logsExporter.finishedLogRecordItems)
                    .isNotEmpty()
            }
        assertThat(wasCalled.get()).isTrue()
        val logs: MutableCollection<LogRecordData> = logsExporter.finishedLogRecordItems
        assertThat(logs).hasSize(1)
        OpenTelemetryAssertions
            .assertThat(logs.iterator().next())
            .hasBody("foo")
            .hasAttributesSatisfyingExactly(
                OpenTelemetryAssertions.equalTo(AttributeKey.stringKey("bing"), "bang"),
                OpenTelemetryAssertions.equalTo(SCREEN_NAME_KEY, CUR_SCREEN_NAME),
                OpenTelemetryAssertions.equalTo(
                    SessionIncubatingAttributes.SESSION_ID,
                    rum.getRumSessionId(),
                ),
            ).hasSeverity(Severity.FATAL3)
    }

    @Ignore("Earlier with mockito cacheDir was null which was causing this TC to pass")
    @Test
    fun diskBufferingEnabled() {
        createAndSetServiceManager()

        val exporterSlot = slot<SpanExporter>()
        val scheduleHandler = mockk<ExportScheduleHandler>(relaxed = true)
        val events = mockk<InitializationEvents>(relaxed = true)
        InitializationEvents.resetForTest()
        InitializationEvents.set(events)

        val config = buildConfig()
        config.setDiskBufferingConfig(DiskBufferingConfig(true))

        RumBuilder
            .builder(application, config)
            .setExportScheduleHandler(scheduleHandler)
            .build()

        Awaitility
            .await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted {
                verify(exactly = 1) { events.spanExporterInitialized(capture(exporterSlot)) }
                verify(exactly = 1) { scheduleHandler.enable() }
                verify(exactly = 0) { scheduleHandler.disable() }
                assertThat(exporterSlot.captured)
                    .isInstanceOf(SpanToDiskExporter::class.java)
            }
    }

    @Test
    fun diskBufferingEnabled_when_exception_thrown() {
        val services: Services = createAndSetServiceManager()
        val cacheStorage = services.cacheStorage
        every { cacheStorage.cacheDir } answers { throw IOException() }

        val exporterSlot = slot<SpanExporter>()
        val scheduleHandler = mockk<ExportScheduleHandler>(relaxed = true)
        val events = mockk<InitializationEvents>(relaxed = true)
        InitializationEvents.resetForTest()
        InitializationEvents.set(events)

        val config = buildConfig()
        config.setDiskBufferingConfig(DiskBufferingConfig(true))

        RumBuilder
            .builder(application, config)
            .setExportScheduleHandler(scheduleHandler)
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
    fun sdkReadyListeners() {
        val config = buildConfig()
        val seen = AtomicReference<OpenTelemetrySdk>()
        createAndSetServiceManager()
        RumBuilder
            .builder(application, config)
            .addOtelSdkReadyListener { newValue: OpenTelemetrySdk -> seen.set(newValue) }
            .build()
        assertThat(seen.get()).isNotNull()
    }

    @Test
    fun diskBufferingDisabled() {
        val exporterSlot = slot<SpanExporter>()
        val scheduleHandler = mockk<ExportScheduleHandler>(relaxed = true)
        val events = mockk<InitializationEvents>(relaxed = true)
        InitializationEvents.resetForTest()
        InitializationEvents.set(events)

        val config = buildConfig()
        config.setDiskBufferingConfig(DiskBufferingConfig())

        RumBuilder
            .builder(application, config)
            .setExportScheduleHandler(scheduleHandler)
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
                AttributeKey.stringKey("someGlobalKey"),
                "someGlobalValue",
            )
        }

        val rum =
            RumBuilder
                .builder(application, otelRumConfig)
                .addLoggerProviderCustomizer { sdkLoggerProviderBuilder: SdkLoggerProviderBuilder, application: Context ->
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
                    .put(SessionIncubatingAttributes.SESSION_ID, rum.getRumSessionId())
                    .put("someGlobalKey", "someGlobalValue")
                    .put("localAttrKey", "localAttrValue")
                    .put(SCREEN_NAME_KEY, CUR_SCREEN_NAME)
                    .build(),
            )
    }

    private fun makeBuilder(): OpenTelemetryRumBuilder = RumBuilder.builder(application, buildConfig())

    private fun buildConfig(): OtelRumConfig = OtelRumConfig().disableNetworkAttributes().disableSdkInitializationEvents()

    companion object {
        const val CUR_SCREEN_NAME: String = "Celebratory Token"

        private fun createAndSetServiceManager(): Services {
            val services = mockk<Services>()
            every { services.appLifecycle } returns mockk<AppLifecycle>()
            every { services.cacheStorage } returns
                mockk<CacheStorage>().apply {
                    every { this@apply.cacheDir } returns File("")
                }
            val screenService = mockk<VisibleScreenTracker>()
            every { screenService.currentlyVisibleScreen } returns CUR_SCREEN_NAME
            every { services.visibleScreenTracker } returns screenService
            set(services)
            return services
        }
    }
}
