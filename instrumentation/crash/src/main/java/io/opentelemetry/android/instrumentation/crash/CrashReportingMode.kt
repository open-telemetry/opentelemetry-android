/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

/**
 * Defines how crash events are reported to OpenTelemetry backends.
 *
 * Different observability backends have different expectations for how exceptions
 * should be reported. Some backends (like traditional log aggregators) expect
 * LogRecords, while others (like APM tools such as AppSignal) expect span events
 * with exception details.
 */
enum class CrashReportingMode {
    /**
     * Emit crashes as LogRecords only.
     * This is the default mode and maintains backward compatibility.
     * The log event name will be "device.crash" with exception attributes.
     */
    LOGS_ONLY,

    /**
     * Emit crashes as Spans with exception events only.
     * Use this mode for backends that only support trace-based error tracking
     * and don't process logs. The span will have status ERROR and include
     * a recorded exception event following OTel semantic conventions.
     */
    SPANS_ONLY,

    /**
     * Emit crashes as both LogRecords and Spans with exception events.
     * Use this mode when you need to send crash data to multiple backends
     * with different expectations (e.g., AppSignal for errors + a log aggregator).
     */
    LOGS_AND_SPANS,
}
