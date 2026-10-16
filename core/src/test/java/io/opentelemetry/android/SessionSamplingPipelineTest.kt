/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig
import io.opentelemetry.android.features.diskbuffering.SignalFromDiskExporter
import io.opentelemetry.android.internal.initialization.InitializationEvents
import io.opentelemetry.android.internal.processors.SessionIdRatioBasedPredicate
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [23, 36])
class SessionSamplingPipelineTest {
    @get:Rule
    val temporary = TemporaryFolder()
    private val keptId = "00000000000000000000000000000001"
    private val droppedId = "00000000000000007fffffffffffffff"
    private val idKey = stringKey("session.id")
    private val decision = SessionIdRatioBasedPredicate(0.5)
    private lateinit var sdk: OpenTelemetrySdk
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setup() {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Services.set(mockk(relaxed = true))
        InitializationEvents.set(mockk(relaxed = true))
        SignalFromDiskExporter.resetForTesting()
    }

    @After
    fun cleanup() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        Services.set(null)
        InitializationEvents.resetForTest()
        SignalFromDiskExporter.resetForTesting()
        clearAllMocks()
    }

    @Test
    fun tracesAndLogsConsultTheSameDecisionAcrossSessionChanges() {
        val current = AtomicReference(keptId)
        val logs = InMemoryLogRecordExporter.create()
        val spans = InMemorySpanExporter.create()
        val provider = SessionProvider { current.get() }
        val builder =
            builder(null, provider, logs)
                .addTracerProviderCustomizer { tracer, _ -> tracer.setSampler(SessionIdRatioBasedSampler(decision, provider)) }
                .addSpanExporterCustomizer { spans }
        builder.logRecordFilter = Predicate { decision.test(checkNotNull(it.getAttribute(idKey))) }
        val rum = builder.build()
        try {
            listOf(keptId, droppedId).forEach { id ->
                current.set(id)
                rum.openTelemetry
                    .getTracer("probe")
                    .spanBuilder(id)
                    .setNoParent()
                    .startSpan()
                    .end()
                rum.emitEvent(id)
            }
            flush()
            assertThat(
                sdk.sdkTracerProvider
                    .forceFlush()
                    .join(5, SECONDS)
                    .isSuccess,
            ).isTrue()
            await().atMost(Duration.ofSeconds(5)).untilAsserted {
                assertThat(logs.finishedLogRecordItems.map { it.attributes.get(idKey) }).containsExactly(keptId)
                assertThat(spans.finishedSpanItems.map { it.attributes.get(idKey) }).containsExactly(keptId)
            }
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun filtersBeforeDiskUsingOriginalSessionIncludingRecoveredCrashes() {
        val directory = temporary.newFolder()
        val current = AtomicReference(keptId)
        val exporter = InMemoryLogRecordExporter.create()
        val calls = mutableListOf<String>()
        val builder = builder(directory, SessionProvider { current.get() }, exporter)
        builder.logRecordFilter =
            Predicate {
                val id = checkNotNull(it.getAttribute(idKey))
                calls.add(id)
                decision.test(id)
            }
        val rum = builder.build()
        try {
            awaitDisk()
            rum.emitEvent("session.start")
            current.set(droppedId)
            rum.emitEvent("dropped-current")
            rum.emitEvent("session.end", attributes = Attributes.of(idKey, keptId))
            rum.emitEvent("app.crash", attributes = Attributes.of(idKey, keptId))
            current.set(keptId)
            rum.emitEvent("dropped-delayed", attributes = Attributes.of(idKey, droppedId))
            flush()
            assertThat(exporter.finishedLogRecordItems).isEmpty()
            await().atMost(Duration.ofSeconds(5)).untilAsserted {
                assertThat(checkNotNull(SignalFromDiskExporter.get()).exportLogsFromDisk()).isTrue()
                assertThat(exporter.finishedLogRecordItems.map { it.eventName })
                    .containsExactly("session.start", "session.end", "app.crash")
            }
            assertThat(exporter.finishedLogRecordItems.map { it.attributes.get(idKey) }).containsOnly(keptId)
            assertThat(calls).containsExactly(keptId, droppedId, keptId, keptId, droppedId)
            checkNotNull(SignalFromDiskExporter.get()).exportLogsFromDisk()
            assertThat(exporter.finishedLogRecordItems).hasSize(3)
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun rejectedRecordsNeverReachDisk() {
        val directory = temporary.newFolder()
        val exporter = InMemoryLogRecordExporter.create()
        val builder = builder(directory, SessionProvider { droppedId }, exporter)
        var calls = 0
        builder.logRecordFilter =
            Predicate {
                calls++
                false
            }
        val rum = builder.build()
        try {
            awaitDisk()
            repeat(100) { rum.emitEvent("rejected") }
            flush()
            assertThat(calls).isEqualTo(100)
            assertThat(File(directory, "logs").walkTopDown().filter { it.isFile }.toList()).isEmpty()
            assertThat(exporter.finishedLogRecordItems).isEmpty()
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun acceptedRecordsAreNotResampledAfterSdkRestart() {
        val directory = temporary.newFolder()
        val firstExport = InMemoryLogRecordExporter.create()
        val firstBuilder = builder(directory, SessionProvider { keptId }, firstExport)
        firstBuilder.logRecordFilter = Predicate { true }
        val first = firstBuilder.build()
        try {
            awaitDisk()
            first.emitEvent("accepted-before-restart")
            flush()
            assertThat(firstExport.finishedLogRecordItems).isEmpty()
        } finally {
            first.shutdown()
            SignalFromDiskExporter.resetForTesting()
        }
        val secondExport = InMemoryLogRecordExporter.create()
        var calls = 0
        val secondBuilder = builder(directory, SessionProvider { droppedId }, secondExport)
        secondBuilder.logRecordFilter =
            Predicate {
                calls++
                false
            }
        val second = secondBuilder.build()
        try {
            awaitDisk()
            second.emitEvent("rejected-after-restart")
            flush()
            await().atMost(Duration.ofSeconds(5)).untilAsserted {
                assertThat(checkNotNull(SignalFromDiskExporter.get()).exportLogsFromDisk()).isTrue()
                assertThat(secondExport.finishedLogRecordItems.map { it.eventName }).containsExactly("accepted-before-restart")
            }
            assertThat(
                secondExport.finishedLogRecordItems
                    .single()
                    .attributes
                    .get(idKey),
            ).isEqualTo(keptId)
            assertThat(calls).isEqualTo(1)
        } finally {
            second.shutdown()
        }
    }

    @Test
    fun noFilterLeavesCurrentAndHistoricalRecordsUnchanged() {
        val exporter = InMemoryLogRecordExporter.create()
        val rum = builder(null, SessionProvider { droppedId }, exporter).build()
        try {
            rum.emitEvent("current")
            rum.emitEvent("crash", attributes = Attributes.of(idKey, keptId))
            flush()
            await().atMost(Duration.ofSeconds(5)).untilAsserted {
                assertThat(exporter.finishedLogRecordItems.map { it.eventName }).containsExactly("current", "crash")
            }
            assertThat(exporter.finishedLogRecordItems.map { it.attributes.get(idKey) }).containsExactly(droppedId, keptId)
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun lookupUsesAttributedSessionEvenIfCurrentSessionChangesDuringLookup() {
        var current = keptId
        var reads = 0
        val exporter = InMemoryLogRecordExporter.create()
        val builder =
            builder(
                null,
                SessionProvider {
                    reads++
                    current
                },
                exporter,
            )
        builder.logRecordFilter =
            Predicate {
                current = droppedId
                decision.test(checkNotNull(it.getAttribute(idKey)))
            }
        val rum = builder.build()
        try {
            rum.emitEvent("kept")
            flush()
            await().atMost(Duration.ofSeconds(5)).untilAsserted {
                assertThat(exporter.finishedLogRecordItems.map { it.attributes.get(idKey) }).containsExactly(keptId)
            }
            assertThat(reads).isEqualTo(1)
            assertThat(current).isEqualTo(droppedId)
        } finally {
            rum.shutdown()
        }
    }

    private fun awaitDisk() {
        await().atMost(Duration.ofSeconds(5)).until { SignalFromDiskExporter.get() != null }
    }

    private fun builder(
        directory: File?,
        provider: SessionProvider,
        exporter: InMemoryLogRecordExporter,
    ): OpenTelemetryRumBuilder {
        val config =
            OtelRumConfig()
                .disableNetworkAttributes()
                .disableScreenAttributes()
                .disableSdkInitializationEvents()
                .disableInstrumentationDiscovery()
        config.setDiskBufferingConfig(
            DiskBufferingConfig(
                enabled = directory != null,
                signalsBufferDir = directory,
                maxFileAgeForWriteMillis = 1,
                minFileAgeForReadMillis = 2,
            ),
        )
        return OpenTelemetryRumBuilder(RuntimeEnvironment.getApplication(), config)
            .setSessionProvider(provider)
            .addOtelReadyListener { sdk = it as OpenTelemetrySdk }
            .setExportScheduleEnablement(mockk(relaxed = true))
            .addLogRecordExporterCustomizer { exporter }
    }

    private fun flush() {
        assertThat(
            sdk.sdkLoggerProvider
                .forceFlush()
                .join(5, SECONDS)
                .isSuccess,
        ).isTrue()
    }
}
