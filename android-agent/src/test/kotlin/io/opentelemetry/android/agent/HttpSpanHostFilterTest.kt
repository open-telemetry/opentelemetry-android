/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.testing.trace.TestSpanData
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.data.StatusData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.function.Predicate

class HttpSpanHostFilterTest {
    @Test
    fun noFilterIsBuiltWithoutAPredicate() {
        assertThat(HttpSpanHostFilter.create(null)).isNull()
    }

    @Test
    fun acceptedHostsAreKeptAndTheRestRejected() {
        val filter = filterFor { it == "api.example.com" }

        assertThat(filter.rejects(httpSpan("api.example.com"))).isFalse()
        assertThat(filter.rejects(httpSpan("analytics.thirdparty.net"))).isTrue()
    }

    @Test
    fun thePredicateSeesALowercasedHost() {
        val filter = filterFor { it == "api.example.com" }

        assertThat(filter.rejects(httpSpan("API.EXAMPLE.COM"))).isFalse()
    }

    @Test
    fun thePredicateSeesTheHostExactlyAsRecorded() {
        // OkHttp records punycode; HttpURLConnection records whatever the caller wrote. The
        // predicate is handed both, unconverted, so it can decide for itself.
        val seen = mutableListOf<String>()
        val filter =
            filterFor {
                seen.add(it)
                true
            }

        filter.rejects(httpSpan("xn--bcher-kva.example"))
        filter.rejects(httpSpan("bücher.example"))

        assertThat(seen).containsExactly("xn--bcher-kva.example", "bücher.example")
    }

    @Test
    fun aPredicateCanExpressASuffixMatch() {
        val filter = filterFor { it == "example.com" || it.endsWith(".example.com") }

        assertThat(filter.rejects(httpSpan("example.com"))).isFalse()
        assertThat(filter.rejects(httpSpan("a.b.example.com"))).isFalse()
        assertThat(filter.rejects(httpSpan("notexample.com"))).isTrue()
        assertThat(filter.rejects(httpSpan("example.com.attacker.net"))).isTrue()
    }

    @Test
    fun spansWithoutAHostAreAlwaysKept() {
        val filter = filterFor { false }

        val noHost = span(SpanKind.CLIENT, Attributes.of(stringKey("http.request.method"), "GET"))

        assertThat(filter.rejects(noHost)).isFalse()
    }

    @Test
    fun onlyHttpSpansAreFiltered() {
        val filter = filterFor { false }

        // gRPC and database client spans also record server.address.
        val grpc = clientSpan("grpc.thirdparty.net", stringKey("rpc.system"), "grpc")
        val database = clientSpan("db.thirdparty.net", stringKey("db.system.name"), "postgresql")

        assertThat(filter.rejects(grpc)).isFalse()
        assertThat(filter.rejects(database)).isFalse()
    }

    @Test
    fun onlyClientSpansAreFiltered() {
        val filter = filterFor { false }

        val serverSpan =
            span(
                SpanKind.SERVER,
                Attributes.of(stringKey("server.address"), "other.example.com", stringKey("http.request.method"), "GET"),
            )

        assertThat(filter.rejects(serverSpan)).isFalse()
    }

    private fun filterFor(predicate: Predicate<String>): HttpSpanHostFilter = checkNotNull(HttpSpanHostFilter.create(predicate))

    private fun httpSpan(serverAddress: String): SpanData = clientSpan(serverAddress, stringKey("http.request.method"), "GET")

    private fun clientSpan(
        serverAddress: String,
        key: AttributeKey<String>,
        value: String,
    ): SpanData =
        span(
            SpanKind.CLIENT,
            Attributes
                .builder()
                .put(stringKey("server.address"), serverAddress)
                .put(key, value)
                .build(),
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
