/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor

internal fun fakeOpenTelemetry(exporter: FakeLogRecordExporter): OpenTelemetry {
    val processor = SimpleLogRecordProcessor.create(exporter)
    val logRecordProcessor = SdkLoggerProvider.builder().addLogRecordProcessor(processor)
    return OpenTelemetrySdk
        .builder()
        .setLoggerProvider(logRecordProcessor.build())
        .build()
}
