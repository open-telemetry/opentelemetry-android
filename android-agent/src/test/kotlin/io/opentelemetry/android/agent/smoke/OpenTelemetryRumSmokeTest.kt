/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.smoke

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer
import io.opentelemetry.android.agent.dsl.OpenTelemetryConfiguration
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenTelemetryRumSmokeTest {
    private val tracerScopeName = "tracer"
    private val spanName = "span-span"
    private val logScopeName = "logger"
    private val logMessage = "Hello world"

    private lateinit var server: FakeOpenTelemetryServer

    @Before
    fun setUp() {
        server = FakeOpenTelemetryServer()
    }

    @Test
    fun testLogExported() {
        performOpenTelemetryRumAction(
            config = {
                httpExport {
                    baseUrl = server.url
                }
                diskBufferingConfig.enabled(false)
            },
            action = {
                recordLog()
            },
        )

        assertLogRequestReceived(server.awaitLogRequest { findLog(it) })
    }

    @Test
    fun testTraceExported() {
        performOpenTelemetryRumAction(
            config = {
                httpExport {
                    baseUrl = server.url
                }
                diskBufferingConfig.enabled(false)
            },
            action = {
                recordSpan()
            },
        )

        assertTraceRequestReceived(server.awaitTraceRequest { findSpan(it) })
    }

    @Test
    fun testSpansNotExportedWhenDisabled() {
        performOpenTelemetryRumAction(
            config = {
                httpExport {
                    baseUrl = server.url
                }
                diskBufferingConfig.enabled(false)
                disableTracing()
            },
            action = {
                recordSpan()
                recordLog()
            },
        )

        server.awaitLogRequest { findLog(it) }
        assertThat(server.traceRequestCount()).isZero
    }

    @Test
    fun testLogsNotExportedWhenDisabled() {
        performOpenTelemetryRumAction(
            config = {
                httpExport {
                    baseUrl = server.url
                }
                diskBufferingConfig.enabled(false)
                disableLogging()
            },
            action = {
                recordLog()
                recordSpan()
            },
        )

        server.awaitTraceRequest { findSpan(it) }
        assertThat(server.logRequestCount()).isZero
    }

    private fun OpenTelemetryRum.recordLog() {
        openTelemetry.logsBridge
            .get(logScopeName)
            .logRecordBuilder()
            .setBody(logMessage)
            .emit()
    }

    private fun OpenTelemetryRum.recordSpan() {
        openTelemetry.tracerProvider
            .get(tracerScopeName)
            .spanBuilder(spanName)
            .startSpan()
            .end()
    }

    private fun findLog(request: ExportLogsServiceRequest) =
        request
            .getResourceLogs(0)
            .scopeLogsList
            .find { scopeLogs ->
                scopeLogs.scope.name == logScopeName
            }?.logRecordsList
            .orEmpty()
            .map { logRecord ->
                logRecord.body.stringValue
            }.contains(logMessage)

    private fun findSpan(request: ExportTraceServiceRequest): Boolean =
        request
            .getResourceSpans(0)
            .getScopeSpans(0)
            .getSpans(0)
            .name == spanName

    private fun assertLogRequestReceived(request: ExportLogsServiceRequest) {
        assertThat(
            request
                .getResourceLogs(0)
                .scopeLogsList
                .first { x ->
                    x.scope.name.equals(logScopeName)
                }.getLogRecords(0)
                .body.stringValue,
        ).isEqualTo(logMessage)
    }

    private fun assertTraceRequestReceived(request: ExportTraceServiceRequest) {
        assertThat(
            request
                .getResourceSpans(0)
                .scopeSpansList
                .first { x ->
                    x.scope.name.equals(tracerScopeName)
                }.getSpans(0)
                .name,
        ).isEqualTo(spanName)
    }

    private fun performOpenTelemetryRumAction(
        config: OpenTelemetryConfiguration.() -> Unit,
        action: OpenTelemetryRum.() -> Unit,
    ) {
        var otelRum: OpenTelemetryRum? = null
        try {
            val ctx = ApplicationProvider.getApplicationContext<Context>()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                otelRum = OpenTelemetryRumInitializer.initialize(ctx, config)
            }

            // perform action on OpenTelemetryRum instance
            otelRum?.action()
        } finally {
            // shutdown OTel
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                otelRum?.shutdown()
            }
        }
    }
}
