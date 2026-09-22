/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.dsl.instrumentation.HttpTelemetryConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpTelemetryConfigurationTest {
    @Test
    fun noHostIsAllowedUntilOneIsNamed() {
        assertThat(HttpTelemetryConfiguration().allowedHosts()).isEmpty()
    }

    @Test
    fun namedHostsAreLowercased() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("API.Example.COM", "cdn.example.com") }

        assertThat(config.allowedHosts()).containsExactlyInAnyOrder("api.example.com", "cdn.example.com")
    }

    @Test
    fun repeatedCallsAddToTheAllowlist() {
        val config =
            HttpTelemetryConfiguration().apply {
                onlyHosts("api.example.com")
                onlyHosts("cdn.example.com")
            }

        assertThat(config.allowedHosts()).containsExactlyInAnyOrder("api.example.com", "cdn.example.com")
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("  api.example.com  ") }

        assertThat(config.allowedHosts()).containsExactly("api.example.com")
    }

    @Test
    fun punycodeHostsAreAccepted() {
        // Punycode is the documented way to allow an internationalized host. xn--fa-hia.de is
        // the UTS-46 spelling of fa\u00df.de, which IDN.toASCII would instead render as fass.de.
        val config = HttpTelemetryConfiguration().apply { onlyHosts("xn--bcher-kva.example", "xn--fa-hia.de") }

        assertThat(config.allowedHosts()).containsExactlyInAnyOrder("xn--bcher-kva.example", "xn--fa-hia.de")
    }

    @Test
    fun hostsThatCouldNeverMatchAreIgnoredWithoutFailingInitialization() {
        val rejected =
            listOf(
                "",
                "   ",
                "exa mple.com",
                "https://api.example.com",
                "api.example.com/v1",
                "api.example.com?q=1",
                "user@api.example.com",
                "api.example.com:8443",
                "[::1]",
                "*.example.com",
                // An internationalized host must be given as punycode; see the KDoc.
                "bücher.example",
                "faß.de",
            )

        for (host in rejected) {
            assertThat(HttpTelemetryConfiguration().apply { onlyHosts(host) }.allowedHosts())
                .describedAs("host '%s'", host)
                .isEmpty()
        }
    }

    @Test
    fun anIgnoredHostDoesNotDiscardTheValidOnesBesideIt() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("api.example.com", "https://bad", "cdn.example.com") }

        assertThat(config.allowedHosts()).containsExactlyInAnyOrder("api.example.com", "cdn.example.com")
    }

    @Test
    fun allowedHostsIsASnapshot() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("api.example.com") }

        val snapshot = config.allowedHosts()
        config.onlyHosts("cdn.example.com")

        assertThat(snapshot).containsExactly("api.example.com")
    }
}
