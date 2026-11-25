/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.sdk.trace.export.SpanExporter

/** An exporter that will filter (not export) spans that fail a predicate.  */
object FilteringSpanExporter {
    /**
     * To migrate, move the filter calls into the [with] configuration block.
     *
     * ```kotlin
     * FilteringSpanExporter.with(delegate) {
     *     rejectSpansNamed("healthcheck")
     * }
     * ```
     */
    @JvmStatic
    @Deprecated("Use FilteringSpanExporter.with to configure the exporter. This method will be removed in 1.11.0.")
    fun builder(delegate: SpanExporter): DeprecatedFilteringSpanExporterBuilder = DeprecatedFilteringSpanExporterBuilder(delegate)

    @JvmStatic
    @JvmName("builder")
    @Deprecated("Retained for binary compatibility until 1.11.0.", level = DeprecationLevel.HIDDEN)
    fun builderForBinaryCompatibility(delegate: SpanExporter): FilteringSpanExporterBuilder = FilteringSpanExporterBuilder(delegate)

    /**
     * Configures span filters using [FilteringSpanExporterBuilder] and returns the exporter.
     *
     * @since 1.10.0
     */
    @JvmStatic
    fun with(
        delegate: SpanExporter,
        build: FilteringSpanExporterBuilder.() -> Unit,
    ): SpanExporter = FilteringSpanExporterBuilder(delegate).apply(build).build()
}
