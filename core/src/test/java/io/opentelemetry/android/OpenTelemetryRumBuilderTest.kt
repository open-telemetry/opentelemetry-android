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
import io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.features.diskbuffering.scheduler.ExportScheduleHandler
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.AndroidInstrumentationLoader
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.internal.AndroidInstrumentationLoaderImpl
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.internal.services.applifecycle.AppLifecycle
import io.opentelemetry.android.internal.services.storage.CacheStorage
import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.context.propagation.TextMapGetter
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.contrib.disk.buffering.SpanToDiskExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.data.AggregationTemporality
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes.SESSION_ID
import org.awaitility.Awaitility.await
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.io.IOException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.function.Function

@RunWith(AndroidJUnit4::class)
class OpenTelemetryRumBuilderTest {
    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var looper: Looper

    @Mock
    private lateinit var applicationContext: Context

    @Mock
    private lateinit var initializationEvents: InitializationEvents

    @Mock
    private lateinit var connectivityManager: ConnectivityManager

    private lateinit var mocks: AutoCloseable

    private val resource =
        Resource
            .getDefault()
            .toBuilder()
            .put("test.attribute", "abcdef")
            .build()
    private val spanExporter = InMemorySpanExporter.create()
    private val logsExporter = InMemoryLogRecordExporter.create()

    @Before
    fun setup() {
        mocks = MockitoAnnotations.openMocks(this)
        `when`(application.applicationContext).thenReturn(applicationContext)
        `when`(application.mainLooper).thenReturn(looper)
        `when`(application.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)
        InitializationEvents.set(initializationEvents)
    }

    @After
    fun tearDown() {
        SignalFromDiskExporter.resetForTesting()
        InitializationEvents.resetForTest()
        AndroidInstrumentationLoader.resetForTest()
        mocks.close()
        Services.set(null)
    }

    @Test
    fun shouldBuildTracerProvider() {
        createAndSetServiceManager()
        val openTelemetryRum =
            makeBuilder()
                .setResource(resource)
                .addTracerProviderCustomizer { tracerProviderBuilder, _ ->
                    tracerProviderBuilder.addSpanProcessor(
                        SimpleSpanProcessor.create(spanExporter),
                    )
                }.build()

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                val sessionId = openTelemetryRum.rumSessionId
                openTelemetryRum
                    .openTelemetry
                    .getTracer("test")
                    .spanBuilder("test span")
                    .startSpan()
                    .end()

                val spans = spanExporter.finishedSpanItems
                assertThat(spans).hasSize(1)
                assertThat(spans[0])
                    .hasName("test span")
                    .hasResource(resource)
                    .hasAttributesSatisfyingExactly(
                        equalTo(SESSION_ID, sessionId),
                        equalTo(SCREEN_NAME_KEY, CUR_SCREEN_NAME),
                    )
            }
    }

    @Test
    fun shouldBuildLogRecordProvider() {
        createAndSetServiceManager()
        val openTelemetryRum =
            makeBuilder()
                .setResource(resource)
                .addLoggerProviderCustomizer { logRecordProviderBuilder, _ ->
                    logRecordProviderBuilder.addLogRecordProcessor(
                        SimpleLogRecordProcessor.create(logsExporter),
                    )
                }.build()

        val attrs = Attributes.of(stringKey("mega"), "hit", stringKey("body.field"), "foo")
        openTelemetryRum.emitEvent("test.event", attrs)

        val logs = logsExporter.finishedLogRecordItems
        assertThat(logs).hasSize(1)
        assertThat(logs[0])
            .hasAttributesSatisfyingExactly(
                equalTo(SESSION_ID, openTelemetryRum.rumSessionId),
                equalTo(SCREEN_NAME_KEY, CUR_SCREEN_NAME),
                equalTo(stringKey("mega"), "hit"),
                equalTo(stringKey("body.field"), "foo"),
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
                .addMeterProviderCustomizer { sdkMeterProviderBuilder, _ ->
                    val metricResAttrs =
                        Attributes.of(stringKey("mmm"), "nnn")
                    sdkMeterProviderBuilder
                        .setResource(Resource.create(metricResAttrs))
                        .registerMetricReader(metricReader)
                }.build()

        val sdk = openTelemetryRum.openTelemetry as OpenTelemetrySdk
        val meter = sdk.sdkMeterProvider.meterBuilder("myMeter").build()
        val counterAttrs = Attributes.of(longKey("adams"), 42L)
        val counter = meter.counterBuilder("myCounter").build()
        counter.add(40, counterAttrs)
        metricReader.forceFlush()
        counter.add(2, counterAttrs)

        val metrics = metricReader.collectAllMetrics().toList()
        assertThat(metrics).hasSize(1)
        assertThat(metrics[0])
            .hasName("myCounter")
            .hasLongSumSatisfying(
                Consumer { sum ->
                    sum.hasPointsSatisfying(
                        Consumer { pt ->
                            pt.hasValue(42L).hasAttributes(counterAttrs)
                        },
                    )
                },
            ).hasResourceSatisfying(
                Consumer { res ->
                    res.hasAttribute(stringKey("mmm"), "nnn")
                },
            )
    }

    @Test
    fun canCustomizeMetricExport() {
        val exporter =
            InMemoryMetricExporter.create(AggregationTemporality.DELTA) // NOT THE DEFAULT
        val periodicReader = PeriodicMetricReader.builder(exporter).build()
        val openTelemetryRum =
            makeBuilder()
                .setResource(resource)
                .addMeterProviderCustomizer { _, _ ->
                    SdkMeterProvider
                        .builder()
                        .registerMetricReader(periodicReader)
                }.addMetricExporterCustomizer { exporter }
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
        assertThat(metrics[0])
            .hasName("FOOCOUNTER")
            .hasLongSumSatisfying(
                Consumer { sum ->
                    sum.hasPointsSatisfying(Consumer { pt -> pt.hasValue(22L) })
                },
            )
        assertThat(metrics[1])
            .hasName("FOOCOUNTER")
            .hasLongSumSatisfying(
                Consumer { sum ->
                    sum.hasPointsSatisfying(Consumer { pt -> pt.hasValue(5L) })
                },
            )
    }

    @Test
    fun shouldInstallInstrumentation() {
        createAndSetServiceManager()
        val sessionProvider = mock(SessionProvider::class.java)
        val localInstrumentation = mock(AndroidInstrumentation::class.java)
        val classpathInstrumentation = mock(AndroidInstrumentation::class.java)

        `when`(localInstrumentation.name).thenReturn("local")
        `when`(classpathInstrumentation.name).thenReturn("classpath")

        val androidInstrumentationServices =
            AndroidInstrumentationLoader.get() as AndroidInstrumentationLoaderImpl
        androidInstrumentationServices.registerForTest(classpathInstrumentation)

        val rum =
            OpenTelemetryRumBuilder(application, buildConfig())
                .addInstrumentation(localInstrumentation)
                .setSessionProvider(sessionProvider)
                .build()

        val expectedCtx =
            InstallationContext(application, rum.openTelemetry, sessionProvider)
        verify(localInstrumentation).install(expectedCtx)
        verify(classpathInstrumentation).install(expectedCtx)
    }

    @Test
    fun shouldInstallInstrumentation_excludingClasspathImplsWhenRequestedInConfig() {
        createAndSetServiceManager()
        val sessionProvider = mock(SessionProvider::class.java)
        val localInstrumentation = mock(AndroidInstrumentation::class.java)
        val classpathInstrumentation = mock(AndroidInstrumentation::class.java)
        val androidInstrumentationServices =
            AndroidInstrumentationLoader.get() as AndroidInstrumentationLoaderImpl
        androidInstrumentationServices.registerForTest(classpathInstrumentation)

        val rum =
            OpenTelemetryRumBuilder(
                application,
                buildConfig().disableInstrumentationDiscovery(),
            ).addInstrumentation(localInstrumentation)
                .setSessionProvider(sessionProvider)
                .build()

        val expectedCtx =
            InstallationContext(application, rum.openTelemetry, sessionProvider)
        verify(localInstrumentation).install(expectedCtx)
        verifyNoInteractions(classpathInstrumentation)
    }

    @Test
    fun canAddPropagator() {
        val context =
            io.opentelemetry.context.Context
                .root()
        val carrier = Any()

        val expected = mock(io.opentelemetry.context.Context::class.java)

        @Suppress("UNCHECKED_CAST")
        val getter = mock(TextMapGetter::class.java) as TextMapGetter<Any>
        val customPropagator = mock(TextMapPropagator::class.java)

        `when`(customPropagator.extract(context, carrier, getter)).thenReturn(expected)

        val rum = makeBuilder().addPropagatorCustomizer { customPropagator }.build()
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
        val customPropagator = mock(TextMapPropagator::class.java)

        val rum = makeBuilder().addPropagatorCustomizer { customPropagator }.build()
        val result = rum.openTelemetry.propagators.textMapPropagator
        assertThat(result).isSameAs(customPropagator)
    }

    @Test
    fun setSpanExporterCustomizer() {
        val exporter = mock(SpanExporter::class.java)
        val wasCalled = AtomicBoolean(false)
        val rum =
            makeBuilder()
                .addSpanExporterCustomizer { _ ->
                    wasCalled.set(true)
                    exporter
                }.build()
        val span =
            rum.openTelemetry
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
        // 5 sec is default
        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted { verify(exporter).export(anyCollection()) }
        assertThat(wasCalled.get()).isTrue()
    }

    @Test
    fun setLogRecordExporterCustomizer() {
        createAndSetServiceManager()
        val wasCalled = AtomicBoolean(false)
        val rum =
            makeBuilder()
                .addLogRecordExporterCustomizer { _ ->
                    wasCalled.set(true)
                    logsExporter
                }.build()

        val logger =
            rum.openTelemetry.logsBridge
                .loggerBuilder("LogScope")
                .build()
        logger
            .logRecordBuilder()
            .setBody("foo")
            .setSeverity(Severity.FATAL3)
            .setAttribute(stringKey("bing"), "bang")
            .emit()
        // 5 sec is default
        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                assertThat(logsExporter.finishedLogRecordItems).isNotEmpty()
            }
        assertThat(wasCalled.get()).isTrue()
        val logs = logsExporter.finishedLogRecordItems
        assertThat(logs).hasSize(1)
        assertThat(logs.iterator().next())
            .hasBody("foo")
            .hasAttributesSatisfyingExactly(
                equalTo(stringKey("bing"), "bang"),
                equalTo(SCREEN_NAME_KEY, CUR_SCREEN_NAME),
                equalTo(SESSION_ID, rum.rumSessionId),
            ).hasSeverity(Severity.FATAL3)
    }

    @Test
    fun diskBufferingEnabled() {
        createAndSetServiceManager()
        val config = buildConfig()
        val scheduleHandler = mock(ExportScheduleHandler::class.java)
        config.setDiskBufferingConfig(DiskBufferingConfig(true))

        OpenTelemetryRum
            .builder(application, config)
            .setExportScheduleHandler(scheduleHandler)
            .build()

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                assertThat(SignalFromDiskExporter.get()).isNotNull()
                verify(scheduleHandler).enable()
                verify(scheduleHandler, never()).disable()
            }
    }

    @Test
    fun diskBufferingEnabled_when_exception_thrown() {
        val services = createAndSetServiceManager()
        val cacheStorage = services.cacheStorage
        val scheduleHandler = mock(ExportScheduleHandler::class.java)
        doAnswer {
            throw IOException()
        }.`when`(cacheStorage).cacheDir

        val config = buildConfig()
        config.setDiskBufferingConfig(DiskBufferingConfig(true))

        OpenTelemetryRum
            .builder(application, config)
            .setExportScheduleHandler(scheduleHandler)
            .build()

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                verify(scheduleHandler, never()).enable()
                verify(scheduleHandler).disable()
                assertThat(SignalFromDiskExporter.get()).isNull()
            }
    }

    @Test
    fun sdkReadyListeners() {
        val config = buildConfig()
        val seen = AtomicReference<OpenTelemetrySdk>()
        createAndSetServiceManager()
        OpenTelemetryRum.builder(application, config).addOtelSdkReadyListener(seen::set).build()
        assertThat(seen.get()).isNotNull()
    }

    @Test
    fun diskBufferingDisabled() {
        createAndSetServiceManager()
        val scheduleHandler = mock(ExportScheduleHandler::class.java)

        val config = buildConfig()
        config.setDiskBufferingConfig(DiskBufferingConfig(false))

        OpenTelemetryRum
            .builder(application, config)
            .setExportScheduleHandler(scheduleHandler)
            .build()

        await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted {
                verify(scheduleHandler, never()).enable()
                verify(scheduleHandler).disable()
                assertThat(SignalFromDiskExporter.get()).isNull()
            }
    }

    @Test
    fun verifyGlobalAttrsForLogs() {
        createAndSetServiceManager()
        val otelRumConfig = buildConfig()
        otelRumConfig.setGlobalAttributes {
            Attributes.of(stringKey("someGlobalKey"), "someGlobalValue")
        }

        val rum =
            OpenTelemetryRum
                .builder(application, otelRumConfig)
                .addLoggerProviderCustomizer { sdkLoggerProviderBuilder, _ ->
                    sdkLoggerProviderBuilder.addLogRecordProcessor(
                        SimpleLogRecordProcessor.create(logsExporter),
                    )
                }.build()

        val logger =
            rum.openTelemetry.logsBridge
                .loggerBuilder("LogScope")
                .build()
        logger.logRecordBuilder().setAttribute(stringKey("localAttrKey"), "localAttrValue").emit()

        val recordedLogs = logsExporter.finishedLogRecordItems
        assertThat(recordedLogs).hasSize(1)
        val logRecordData = recordedLogs[0]
        assertThat(logRecordData)
            .hasAttributes(
                Attributes
                    .builder()
                    .put(SESSION_ID, rum.rumSessionId)
                    .put("someGlobalKey", "someGlobalValue")
                    .put("localAttrKey", "localAttrValue")
                    .put(SCREEN_NAME_KEY, CUR_SCREEN_NAME)
                    .build(),
            )
    }

    private fun makeBuilder(): OpenTelemetryRumBuilder = OpenTelemetryRum.builder(application, buildConfig())

    private fun buildConfig(): OtelRumConfig = OtelRumConfig().disableNetworkAttributes().disableSdkInitializationEvents()

    companion object {
        const val CUR_SCREEN_NAME = "Celebratory Token"

        private fun createAndSetServiceManager(): Services {
            val services = mock(Services::class.java)
            `when`(services.appLifecycle).thenReturn(mock(AppLifecycle::class.java))
            `when`(services.cacheStorage).thenReturn(mock(CacheStorage::class.java))
            val screenService = mock(VisibleScreenTracker::class.java)
            `when`(screenService.currentlyVisibleScreen).thenReturn(CUR_SCREEN_NAME)
            `when`(services.visibleScreenTracker).thenReturn(screenService)
            Services.set(services)
            return services
        }
    }
}
