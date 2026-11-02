/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.kotlin.semconv.ServerAttributes
import io.opentelemetry.sdk.logs.data.LogRecordData

internal class WebsocketEventHostFilter private constructor(
    private val recordEventForHost: (String) -> Boolean,
) {
    fun rejects(record: LogRecordData): Boolean {
        if (record.instrumentationScopeInfo.name != WEBSOCKET_SCOPE) {
            return false
        }
        val host = record.attributes.get(SERVER_ADDRESS) ?: return false
        return !recordEventForHost(host.lowercase())
    }

    companion object {
        private const val WEBSOCKET_SCOPE = "io.opentelemetry.websocket.events"
        private val SERVER_ADDRESS = AttributeKey.stringKey(ServerAttributes.SERVER_ADDRESS)

        fun create(recordEventForHost: ((String) -> Boolean)?): WebsocketEventHostFilter? =
            recordEventForHost?.let { WebsocketEventHostFilter(it) }
    }
}
