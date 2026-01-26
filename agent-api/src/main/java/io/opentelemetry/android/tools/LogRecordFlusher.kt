/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.tools

import io.opentelemetry.sdk.common.CompletableResultCode

interface LogRecordFlusher {
    fun flushLogRecords(): CompletableResultCode
}
