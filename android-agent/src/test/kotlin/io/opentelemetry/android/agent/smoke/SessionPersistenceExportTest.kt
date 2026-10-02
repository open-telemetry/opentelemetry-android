/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.smoke

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.android.agent.session.SessionStorage
import io.opentelemetry.api.common.AttributeKey.stringKey
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.zip.GZIPInputStream

@OptIn(Incubating::class)
@RunWith(AndroidJUnit4::class)
class SessionPersistenceExportTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `recreated SDK exports linked starts and keeps recovered logs on their original session`() {
        val spans = CopyOnWriteArrayList<Span>()
        val logs = CopyOnWriteArrayList<LogRecord>()
        val received = CountDownLatch(5)
        val file = File(temporary.root, "session")
        MockWebServer().use { server ->
            server.dispatcher =
                object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        GZIPInputStream(checkNotNull(request.body).toByteArray().inputStream()).use { body ->
                            when (request.target) {
                                "/v1/traces" -> {
                                    val batch =
                                        ExportTraceServiceRequest
                                            .parseFrom(body)
                                            .resourceSpansList
                                            .flatMap { it.scopeSpansList }
                                            .flatMap { it.spansList }
                                    spans.addAll(batch)
                                    batch.forEach { if (it.name.startsWith("launch-")) received.countDown() }
                                }

                                "/v1/logs" -> {
                                    val batch =
                                        ExportLogsServiceRequest
                                            .parseFrom(body)
                                            .resourceLogsList
                                            .flatMap { it.scopeLogsList }
                                            .flatMap { it.logRecordsList }
                                    logs.addAll(batch)
                                    batch.forEach {
                                        if (it.eventName.startsWith("launch-") ||
                                            it.eventName == "recovered-crash"
                                        ) {
                                            received.countDown()
                                        }
                                    }
                                }

                                else -> {
                                    error("Unexpected path ${request.target}")
                                }
                            }
                        }
                        return MockResponse(200)
                    }
                }
            server.start()
            val ids = mutableListOf<String>()
            repeat(2) { launch ->
                ids.add(recordLaunch(server, file, launch, ids.firstOrNull()))
            }
            assertThat(received.await(10, SECONDS)).isTrue()
            assertThat(ids.distinct()).hasSize(2)
            assertThat(SessionStorage.file(file).get().id).isEqualTo(ids[1])
            ids.forEachIndexed { launch, id ->
                assertThat(spans.single { it.name == "launch-$launch" }.attributesList.value("session.id")).isEqualTo(id)
                assertThat(logs.single { it.eventName == "launch-$launch" }.attributesList.value("session.id")).isEqualTo(id)
            }
            val starts = logs.filter { it.eventName == "session.start" }
            assertThat(starts).hasSize(2)
            assertThat(
                starts.single { it.attributesList.value("session.id") == ids[0] }.attributesList.value("session.previous_id"),
            ).isNull()
            assertThat(starts.single { it.attributesList.value("session.id") == ids[1] }.attributesList.value("session.previous_id"))
                .isEqualTo(ids[0])
            assertThat(logs.filter { it.eventName == "session.end" }).isEmpty()
            assertThat(logs.single { it.eventName == "recovered-crash" }.attributesList.value("session.id")).isEqualTo(ids[0])
        }
    }

    private fun recordLaunch(
        server: MockWebServer,
        file: File,
        launch: Int,
        previousId: String?,
    ): String {
        val rum =
            OpenTelemetryRumInitializer.initialize(RuntimeEnvironment.getApplication()) {
                httpExport { baseUrl = server.url("/").toString() }
                diskBuffering { enabled(false) }
                disableMetrics()
                session {
                    storage(SessionStorage.file(file))
                    linkPreviousSessionOnRestart = true
                }
            }
        try {
            val id = rum.sessionProvider.getSessionId()
            rum.openTelemetry
                .getTracer("restart-test")
                .spanBuilder("launch-$launch")
                .startSpan()
                .end()
            rum.emitEvent("launch-$launch")
            if (previousId != null) {
                rum.emitEvent(
                    "recovered-crash",
                    attributes =
                        io.opentelemetry.api.common.Attributes
                            .of(stringKey("session.id"), previousId),
                )
            }
            return id
        } finally {
            rum.shutdown()
        }
    }

    private fun List<KeyValue>.value(key: String): String? = firstOrNull { it.key == key }?.value?.stringValue
}
