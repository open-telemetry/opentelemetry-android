/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.contrib.interceptor.InterceptableLogRecordExporter
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WebsocketEventHostFilterTest {
    @Test
    fun noFilterIsBuiltWithoutAPredicate() {
        assertThat(WebsocketEventHostFilter.create(null)).isNull()
    }

    @Test
    fun filtersEveryWebsocketEventTypeAndSupportsDenylists() {
        val records = records()
        val allowlist = checkNotNull(WebsocketEventHostFilter.create { it == "api.example.com" })
        val denylist = checkNotNull(WebsocketEventHostFilter.create { it != "api.example.com" })

        assertThat(records.take(4)).allSatisfy {
            assertThat(allowlist.rejects(it)).isFalse()
            assertThat(denylist.rejects(it)).isTrue()
        }
        assertThat(allowlist.rejects(records[4])).isTrue()
        assertThat(denylist.rejects(records[4])).isFalse()
    }

    @Test
    fun passesLowercasedHostsWithoutFurtherNormalization() {
        val seen = mutableListOf<String>()
        val filter =
            checkNotNull(
                WebsocketEventHostFilter.create {
                    seen.add(it)
                    true
                },
            )
        records().forEach { filter.rejects(it) }

        assertThat(seen).containsExactly(
            "api.example.com",
            "api.example.com",
            "api.example.com",
            "api.example.com",
            "xn--bcher-kva.example",
        )
    }

    @Test
    fun mixedExportBatchesKeepUnrelatedLogsAndRecordsWithoutHosts() {
        val filter = checkNotNull(WebsocketEventHostFilter.create { false })
        val delegate = InMemoryLogRecordExporter.create()
        val exporter = InterceptableLogRecordExporter(delegate) { if (filter.rejects(it)) null else it }
        try {
            val records = records()
            assertThat(exporter.export(records).isSuccess).isTrue()
            assertThat(delegate.finishedLogRecordItems).containsExactlyElementsOf(records.takeLast(2))
            assertThat(exporter.flush().isSuccess).isTrue()
        } finally {
            exporter.shutdown()
        }
    }

    private fun records(): List<LogRecordData> {
        val exporter = InMemoryLogRecordExporter.create()
        SdkLoggerProvider
            .builder()
            .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter))
            .build()
            .use { provider ->
                val logger = provider.get("io.opentelemetry.websocket.events")
                for (event in listOf("websocket.open", "websocket.message", "websocket.close", "websocket.error")) {
                    logger
                        .logRecordBuilder()
                        .setEventName(event)
                        .setAttribute(stringKey("server.address"), "API.EXAMPLE.COM")
                        .emit()
                }
                logger
                    .logRecordBuilder()
                    .setEventName("websocket.open")
                    .setAttribute(stringKey("server.address"), "xn--bcher-kva.example")
                    .emit()
                logger.logRecordBuilder().setEventName("websocket.error").emit()
                provider
                    .get("application")
                    .logRecordBuilder()
                    .setEventName("websocket.open")
                    .setAttribute(stringKey("server.address"), "other.example.com")
                    .emit()
                return exporter.finishedLogRecordItems.toList()
            }
    }
}
