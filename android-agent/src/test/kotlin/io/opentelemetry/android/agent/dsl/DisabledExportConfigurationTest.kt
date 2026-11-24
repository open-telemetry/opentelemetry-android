/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.mockk.mockk
import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.FakeClock
import io.opentelemetry.android.agent.FakeInstrumentationLoader
import io.opentelemetry.android.agent.connectivity.ClientTlsConnectivity
import io.opentelemetry.android.agent.connectivity.SSLContextConnectivity
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

@OptIn(Incubating::class)
internal class DisabledExportConfigurationTest {
    @Test
    fun `TLS validation runs only when at least one signal is enabled`() {
        listOf("none", "traces", "logs", "metrics").forEach { enabledSignal ->
            val config =
                OpenTelemetryConfiguration(clock = FakeClock(), instrumentationLoader = FakeInstrumentationLoader()).apply {
                    if (enabledSignal != "traces") disableTracing()
                    if (enabledSignal != "logs") disableLogging()
                    if (enabledSignal != "metrics") disableMetrics()
                    httpExport {
                        sslContext = SSLContextConnectivity(mockk(), mockk())
                        clientTls = ClientTlsConnectivity(byteArrayOf(1), byteArrayOf(2))
                    }
                }

            if (enabledSignal == "none") {
                assertThatCode { config.validateExportConfiguration() }.doesNotThrowAnyException()
            } else {
                assertThatThrownBy { config.validateExportConfiguration() }
                    .describedAs(enabledSignal)
                    .isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessage("sslContext and clientTls cannot be combined")
            }
        }
    }
}
