/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import android.content.Context
import com.google.auto.service.AutoService
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor

/** Entrypoint for installing the crash reporting instrumentation.  */
@AutoService(AndroidInstrumentation::class)
class CrashReporterInstrumentation : AndroidInstrumentation {
    private val additionalExtractors: MutableList<EventAttributesExtractor<CrashDetails>> =
        mutableListOf()
    private var crashReporter: CrashReporter? = null
    private var runtimeDetailsExtractor: RuntimeDetailsExtractor? = null

    /** Adds an [EventAttributesExtractor] that will extract additional attributes.  */
    fun addAttributesExtractor(extractor: EventAttributesExtractor<CrashDetails>) {
        additionalExtractors.add(extractor)
    }

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        if (crashReporter != null) {
            return
        }
        val runtimeExtractor = RuntimeDetailsExtractor.create(context)
        runtimeDetailsExtractor = runtimeExtractor
        val allExtractors = listOf(runtimeExtractor) + additionalExtractors
        val reporter = CrashReporter(allExtractors)
        reporter.install(openTelemetryRum.openTelemetry)
        crashReporter = reporter
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        crashReporter?.uninstall()
        crashReporter = null

        runtimeDetailsExtractor?.unregister(context)
        runtimeDetailsExtractor = null
    }

    override val name: String = "java-crash"
}
