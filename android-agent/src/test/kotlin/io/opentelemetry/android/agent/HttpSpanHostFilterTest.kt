/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.testing.trace.TestSpanData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpSpanHostFilterTest {
    @Test
    fun noFilterIsBuiltWhenNoHostIsConfigured() {
        assertThat(HttpSpanHostFilter.create(emptySet())).isNull()
    }

    @Test
    fun namedHostsAreKeptAndTheRestRejected() {
        val filter = filterFor("api.example.com", "cdn.example.com")

        assertThat(filter.rejects(httpSpan("api.example.com"))).isFalse()
        assertThat(filter.rejects(httpSpan("cdn.example.com"))).isFalse()
        assertThat(filter.rejects(httpSpan("analytics.thirdparty.net"))).isTrue()
    }

    @Test
    fun hostComparisonIgnoresCase() {
        val filter = filterFor("api.example.com")

        assertThat(filter.rejects(httpSpan("API.EXAMPLE.COM"))).isFalse()
    }

    @Test
    fun hostComparisonIsExactSoNeighbouringNamesDoNotMatch() {
        val filter = filterFor("example.com")

        assertThat(filter.rejects(httpSpan("api.example.com"))).isTrue()
        assertThat(filter.rejects(httpSpan("notexample.com"))).isTrue()
        assertThat(filter.rejects(httpSpan("example.com.attacker.net"))).isTrue()
    }

    @Test
    fun punycodeConfigMatchesBothSpellingsOfTheRecordedHost() {
        // OkHttp records punycode; HttpURLConnection records whatever the caller wrote.
        val filter = filterFor("xn--bcher-kva.example")

        assertThat(filter.rejects(httpSpan("xn--bcher-kva.example"))).isFalse()
        assertThat(filter.rejects(httpSpan("bücher.example"))).isFalse()
        assertThat(filter.rejects(httpSpan("other.example"))).isTrue()
    }

    @Test
    fun recordedHostWithNoPunycodeFormIsRejectedRatherThanThrowing() {
        val filter = filterFor("api.example.com")

        assertThat(filter.rejects(httpSpan("bücher..example"))).isTrue()
    }

    @Test
    fun spansWithoutAHostAreAlwaysKept() {
        val filter = filterFor("api.example.com")

        val noHost = span(SpanKind.CLIENT, Attributes.of(stringKey("http.request.method"), "GET"))

        assertThat(filter.rejects(noHost)).isFalse()
    }

    @Test
    fun onlyHttpSpansAreFiltered() {
        val filter = filterFor("api.example.com")

        // gRPC and database client spans also record server.address.
        val grpc = clientSpan("grpc.thirdparty.net", stringKey("rpc.system") to "grpc")
        val database = clientSpan("db.thirdparty.net", stringKey("db.system.name") to "postgresql")

        assertThat(filter.rejects(grpc)).isFalse()
        assertThat(filter.rejects(database)).isFalse()
    }

    @Test
    fun onlyClientSpansAreFiltered() {
        val filter = filterFor("api.example.com")

        val serverSpan =
            span(
                SpanKind.SERVER,
                Attributes.of(stringKey("server.address"), "other.example.com", stringKey("http.request.method"), "GET"),
            )

        assertThat(filter.rejects(serverSpan)).isFalse()
    }

    private fun filterFor(vararg hosts: String): HttpSpanHostFilter = checkNotNull(HttpSpanHostFilter.create(hosts.toSet()))

    private fun httpSpan(serverAddress: String): SpanData =
        span(
            SpanKind.CLIENT,
            Attributes.of(stringKey("server.address"), serverAddress, stringKey("http.request.method"), "GET"),
        )

    private fun clientSpan(
        serverAddress: String,
        extra: Pair<AttributeKeyString, String>,
    ): SpanData =
        span(
            SpanKind.CLIENT,
            Attributes.of(stringKey("server.address"), serverAddress, extra.first, extra.second),
        )

    private fun span(
        kind: SpanKind,
        attributes: Attributes,
    ): SpanData =
        TestSpanData
            .builder()
            .setName("GET")
            .setKind(kind)
            .setStatus(StatusData.unset())
            .setHasEnded(true)
            .setStartEpochNanos(0)
            .setEndEpochNanos(123)
            .setAttributes(attributes)
            .build()
}

private typealias AttributeKeyString = io.opentelemetry.api.common.AttributeKey<String>
