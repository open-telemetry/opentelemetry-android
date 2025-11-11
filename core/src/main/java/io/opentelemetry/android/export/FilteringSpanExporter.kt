/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.sdk.trace.export.SpanExporter

/** An exporter that will filter (not export) spans that fail a predicate.  */
object FilteringSpanExporter {
    @JvmStatic
    fun builder(delegate: SpanExporter): FilteringSpanExporterBuilder = FilteringSpanExporterBuilder(delegate)
}
