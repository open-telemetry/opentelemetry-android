/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.dsl.instrumentation.HttpTelemetryConfiguration
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.testing.trace.TestSpanData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class HttpTelemetryConfigurationTest {
    @Test
    fun keepsEveryHostUntilOneIsNamed() {
        val config = HttpTelemetryConfiguration()

        assertThat(config.keepsEveryHost()).isTrue()
        assertThat(config.rejects(httpSpan("anything.example.com"))).isFalse()

        config.onlyHosts("api.example.com")

        assertThat(config.keepsEveryHost()).isFalse()
    }

    @Test
    fun onlyHostsKeepsNamedHostsAndRejectsTheRest() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("api.example.com", "cdn.example.com") }

        assertThat(config.rejects(httpSpan("api.example.com"))).isFalse()
        assertThat(config.rejects(httpSpan("cdn.example.com"))).isFalse()
        assertThat(config.rejects(httpSpan("analytics.thirdparty.net"))).isTrue()
    }

    @Test
    fun hostComparisonIgnoresCase() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("API.Example.COM") }

        assertThat(config.rejects(httpSpan("api.example.com"))).isFalse()
        assertThat(config.rejects(httpSpan("API.EXAMPLE.COM"))).isFalse()
    }

    @Test
    fun hostComparisonIsExactSoNeighbouringNamesDoNotMatch() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("example.com") }

        assertThat(config.rejects(httpSpan("api.example.com"))).isTrue()
        assertThat(config.rejects(httpSpan("notexample.com"))).isTrue()
        assertThat(config.rejects(httpSpan("example.com.attacker.net"))).isTrue()
    }

    @Test
    fun repeatedCallsAddToTheAllowlist() {
        val config =
            HttpTelemetryConfiguration().apply {
                onlyHosts("api.example.com")
                onlyHosts("cdn.example.com")
            }

        assertThat(config.rejects(httpSpan("api.example.com"))).isFalse()
        assertThat(config.rejects(httpSpan("cdn.example.com"))).isFalse()
    }

    @Test
    fun punycodeConfigMatchesBothSpellingsOfTheRecordedHost() {
        // OkHttp records punycode; HttpURLConnection records whatever the caller wrote.
        val config = HttpTelemetryConfiguration().apply { onlyHosts("xn--bcher-kva.example") }

        assertThat(config.rejects(httpSpan("xn--bcher-kva.example"))).isFalse()
        assertThat(config.rejects(httpSpan("b\u00fccher.example"))).isFalse()
        assertThat(config.rejects(httpSpan("other.example"))).isTrue()
    }

    @Test
    fun internationalizedHostMustBeConfiguredAsPunycode() {
        // IDN.toASCII applies IDNA2003 and yields fass.de, while OkHttp applies UTS-46 and
        // yields xn--fa-hia.de, so converting here would silently never match.
        for (host in listOf("b\u00fccher.example", "fa\u00df.de")) {
            assertThatThrownBy { HttpTelemetryConfiguration().onlyHosts(host) }
                .describedAs("host '%s'", host)
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("punycode")
        }
    }

    @Test
    fun onlyHttpClientSpansAreFiltered() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("api.example.com") }

        // gRPC and database client spans also record server.address.
        assertThat(
            config.rejects(clientSpan(Attributes.of(stringKey("server.address"), "grpc.thirdparty.net", stringKey("rpc.system"), "grpc"))),
        ).isFalse()
        assertThat(
            config.rejects(
                clientSpan(Attributes.of(stringKey("server.address"), "db.thirdparty.net", stringKey("db.system.name"), "postgresql")),
            ),
        ).isFalse()
    }

    @Test
    fun nonClientHttpSpansAreKept() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("api.example.com") }

        val serverSpan =
            TestSpanData
                .builder()
                .setName("GET")
                .setKind(SpanKind.SERVER)
                .setStatus(StatusData.unset())
                .setHasEnded(true)
                .setStartEpochNanos(0)
                .setEndEpochNanos(123)
                .setAttributes(
                    Attributes.of(stringKey("server.address"), "other.example.com", stringKey("http.request.method"), "GET"),
                ).build()

        assertThat(config.rejects(serverSpan)).isFalse()
    }

    @Test
    fun recordedHostWithNoPunycodeFormIsRejectedRatherThanThrowing() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("api.example.com") }

        assertThat(config.rejects(httpSpan("b\u00fccher..example"))).isTrue()
    }

    @Test
    fun spansWithoutAHostAreAlwaysKept() {
        val config = HttpTelemetryConfiguration().apply { onlyHosts("api.example.com") }

        val spanWithNoHost =
            span(Attributes.of(stringKey("app.screen.name"), "Checkout"))

        assertThat(config.rejects(spanWithNoHost)).isFalse()
    }

    @Test
    fun hostsThatCouldNeverMatchAreRejectedAtConfigurationTime() {
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
                "fa\u00df.de",
                // IDN.toASCII rejects empty labels.
                "b\u00fccher..example",
                ".b\u00fccher.example",
            )

        for (host in rejected) {
            assertThatThrownBy { HttpTelemetryConfiguration().onlyHosts(host) }
                .describedAs("host '%s'", host)
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Invalid host name")
        }
    }

    private fun httpSpan(serverAddress: String): SpanData =
        clientSpan(Attributes.of(stringKey("server.address"), serverAddress, stringKey("http.request.method"), "GET"))

    private fun clientSpan(attributes: Attributes): SpanData = span(attributes)

    private fun span(attributes: Attributes): SpanData =
        TestSpanData
            .builder()
            .setName("GET")
            .setKind(SpanKind.CLIENT)
            .setStatus(StatusData.unset())
            .setHasEnded(true)
            .setStartEpochNanos(0)
            .setEndEpochNanos(123)
            .setAttributes(attributes)
            .build()
}
