/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.smoketestapp

import android.app.Activity
import android.os.Bundle
import io.opentelemetry.android.agent.OpenTelemetryRumInitializer

const val OTLP_ENDPOINT_EXTRA = "io.opentelemetry.android.smoketest.OTLP_ENDPOINT"
const val SMOKE_TEST_SCOPE_NAME = "smoke-test"
const val SMOKE_TEST_SPAN_NAME = "minified-app-smoke-test"

class SmokeTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val endpoint =
            requireNotNull(intent.getStringExtra(OTLP_ENDPOINT_EXTRA)) {
                "Missing OTLP endpoint"
            }
        val openTelemetryRum =
            OpenTelemetryRumInitializer.initialize(application) {
                httpExport {
                    baseUrl = endpoint
                }
                diskBuffering {
                    enabled(false)
                }
                disableLogging()
                disableMetrics()
            }

        try {
            openTelemetryRum.openTelemetry
                .tracerProvider
                .get(SMOKE_TEST_SCOPE_NAME)
                .spanBuilder(SMOKE_TEST_SPAN_NAME)
                .startSpan()
                .end()
        } finally {
            // Shutdown starts the exporter flush; the instrumentation test waits for its request.
            openTelemetryRum.shutdown()
        }
    }
}
