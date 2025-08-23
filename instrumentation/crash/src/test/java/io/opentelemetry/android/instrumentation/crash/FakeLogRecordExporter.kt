/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A fake implementation of [LogRecordExporter] that is used in tests to assert
 * that logs are exported as expected.
 */
internal class FakeLogRecordExporter : LogRecordExporter {
    private val logs: MutableList<LogRecordData> = mutableListOf()

    override fun export(logs: Collection<LogRecordData>): CompletableResultCode {
        this.logs += logs
        return CompletableResultCode.ofSuccess()
    }

    override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

    override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()

    fun awaitLogs(
        count: Int,
        timeoutMs: Int = 10000,
    ): List<LogRecordData> {
        val countDownLatch = CountDownLatch(1)

        repeat(timeoutMs) {
            if (logs.size < count) {
                countDownLatch.await(1, TimeUnit.MILLISECONDS)
            } else {
                return logs.toList()
            }
        }
        throw TimeoutException("Failed to fetch logs in given timeout.")
    }
}
