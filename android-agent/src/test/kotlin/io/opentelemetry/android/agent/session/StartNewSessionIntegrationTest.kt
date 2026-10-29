/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.IncubatingOpenTelemetryRum
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.android.internal.services.Services
import io.opentelemetry.android.semconv.events.SessionEndEvent.Companion.SESSION_END_EVENT_NAME
import io.opentelemetry.android.semconv.events.SessionStartEvent.Companion.SESSION_START_EVENT_NAME
import io.opentelemetry.android.session.Session
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.logs.v1.LogRecord
import io.opentelemetry.proto.trace.v1.Span
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.zip.GZIPInputStream

@OptIn(Incubating::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [23, 36])
class StartNewSessionIntegrationTest {
    private var previousExceptionHandler: Thread.UncaughtExceptionHandler? = null

    @Before
    fun setUp() {
        previousExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        val services = mockk<Services>(relaxed = true)
        every { services.appLifecycle } returns mockk(relaxed = true)
        every { services.visibleScreenTracker } returns mockk(relaxed = true)
        Services.set(services)
    }

    @After
    fun tearDown() {
        Services.set(null)
        Thread.setDefaultUncaughtExceptionHandler(previousExceptionHandler)
    }

    @Test
    fun `agent with existing custom provider rejects replacement and leaves its session unchanged`() {
        val provider = SessionProvider { "custom-session" }
        val rum = createRum(provider)
        try {
            assertThatThrownBy { rum.startNewSession() }.isInstanceOf(IllegalStateException::class.java)
            assertThat(rum.sessionProvider).isSameAs(provider)
            assertThat(provider.getSessionId()).isEqualTo("custom-session")
        } finally {
            rum.shutdown()
        }
    }

    @Test
    fun `agent cannot replace a manager explicitly supplied by another agent`() {
        val owner = createRum()
        try {
            val first = owner.sessionProvider.getSessionId()
            val borrower = createRum(owner.sessionProvider)
            try {
                assertThat(borrower.sessionProvider).isSameAs(owner.sessionProvider)
                assertThatThrownBy { borrower.startNewSession() }.isInstanceOf(IllegalStateException::class.java)
                assertThat(owner.sessionProvider.getSessionId()).isEqualTo(first)
                assertThat(owner.startNewSession()).isTrue()
                assertThat(borrower.sessionProvider.getSessionId()).isNotEqualTo(first)
            } finally {
                borrower.shutdown()
            }
        } finally {
            owner.shutdown()
        }
    }

    private fun createRum(provider: SessionProvider? = null): IncubatingOpenTelemetryRum =
        OpenTelemetryRumInitializer.initialize(RuntimeEnvironment.getApplication()) {
            disableTracing()
            disableLogging()
            disableMetrics()
            diskBuffering { enabled(false) }
            session { sessionProvider = provider }
        } as IncubatingOpenTelemetryRum

    @Test
    fun `manual replacement exports linked events and keeps telemetry with its original session`() {
        val logs = ConcurrentLinkedQueue<LogRecord>()
        val spans = ConcurrentLinkedQueue<Span>()
        val received = CountDownLatch(2)
        val saved = mutableListOf<Session>()
        val storage = mockk<SessionStorage>()
        every { storage.getPreviousSession() } returns null
        every { storage.save(capture(saved)) } returns Unit
        MockWebServer().use { server ->
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        GZIPInputStream(checkNotNull(request.body).toByteArray().inputStream()).use { body ->
                            when (request.target) {
                                "/v1/logs" -> {
                                    val batch =
                                        ExportLogsServiceRequest
                                            .parseFrom(body)
                                            .resourceLogsList
                                            .flatMap { it.scopeLogsList }
                                            .flatMap { it.logRecordsList }
                                    logs.addAll(batch)
                                    if (batch.any { it.body.stringValue == "after-reset" }) received.countDown()
                                }

                                "/v1/traces" -> {
                                    val batch =
                                        ExportTraceServiceRequest
                                            .parseFrom(body)
                                            .resourceSpansList
                                            .flatMap { it.scopeSpansList }
                                            .flatMap { it.spansList }
                                    spans.addAll(batch)
                                    if (batch.any { it.name == "after-reset" }) received.countDown()
                                }
                            }
                        }
                        return MockResponse(200)
                    }
                }
            server.start()
            val rum =
                OpenTelemetryRumInitializer.initialize(RuntimeEnvironment.getApplication()) {
                    httpExport { baseUrl = server.url("/").toString() }
                    diskBuffering { enabled(false) }
                    disableMetrics()
                    session { storage(storage) }
                }
            val first: String
            val second: String
            try {
                first = rum.sessionProvider.getSessionId()
                val tracer = rum.openTelemetry.getTracer("start-new-session-test")
                val logger = rum.openTelemetry.logsBridge.get("start-new-session-test")
                val existingSpan = tracer.spanBuilder("before-reset").startSpan()
                logger.logRecordBuilder().setBody("before-reset").emit()
                assertThat((rum as IncubatingOpenTelemetryRum).startNewSession()).isTrue()
                second = saved.last().id
                assertThat(second).isNotEqualTo(first)
                assertThat(rum.sessionProvider.getSessionId()).isEqualTo(second)
                assertThat(saved.map { it.id }).containsExactly(first, second)
                existingSpan.end()
                tracer.spanBuilder("after-reset").startSpan().end()
                logger.logRecordBuilder().setBody("after-reset").emit()
            } finally {
                rum.shutdown()
            }
            assertThat(received.await(10, SECONDS)).isTrue()
            assertThat(spans.single { it.name == "before-reset" }.attributesList.value("session.id")).isEqualTo(first)
            assertThat(spans.single { it.name == "after-reset" }.attributesList.value("session.id")).isEqualTo(second)
            assertThat(logs.single { it.body.stringValue == "before-reset" }.attributesList.value("session.id")).isEqualTo(first)
            assertThat(logs.single { it.body.stringValue == "after-reset" }.attributesList.value("session.id")).isEqualTo(second)
            assertThat(logs.single { it.eventName == SESSION_END_EVENT_NAME }.attributesList.value("session.id")).isEqualTo(first)
            val linkedStart =
                logs.single {
                    it.eventName == SESSION_START_EVENT_NAME && it.attributesList.value("session.id") == second
                }
            assertThat(linkedStart.attributesList.value("session.previous_id")).isEqualTo(first)
        }
    }

    private fun List<KeyValue>.value(key: String): String? = find { it.key == key }?.value?.stringValue
}
