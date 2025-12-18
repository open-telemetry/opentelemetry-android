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
                val logger = openTelemetry.logsBridge.get("logger")
                logger.logRecordBuilder().setBody("Hello world").emit()
            },
        )

        val logRequest = server.awaitLogRequest()
        assertLogRequestReceived(logRequest)
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
                val tracer = openTelemetry.tracerProvider.get("tracer")
                tracer.spanBuilder("span").startSpan().end()
            },
        )

        val traceRequest =
            server.awaitTraceRequest {
                it
                    .getResourceSpans(0)
                    .getScopeSpans(0)
                    .getSpans(0)
                    .name == "span"
            }
        assertTraceRequestReceived(traceRequest)
    }

    private fun assertLogRequestReceived(request: ExportLogsServiceRequest) {
        assertThat(
            request
                .getResourceLogs(0)
                .scopeLogsList
                .first { x ->
                    x.scope.name.equals("logger")
                }.getLogRecords(0)
                .body.stringValue,
        ).isEqualTo("Hello world")
    }

    private fun assertTraceRequestReceived(request: ExportTraceServiceRequest) {
        assertThat(
            request
                .getResourceSpans(0)
                .scopeSpansList
                .first { x ->
                    x.scope.name.equals("tracer")
                }.getSpans(0)
                .name,
        ).isEqualTo("span")
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
