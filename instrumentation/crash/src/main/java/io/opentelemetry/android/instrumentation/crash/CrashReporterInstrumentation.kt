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

    /** Adds an [EventAttributesExtractor] that will extract additional attributes.  */
    fun addAttributesExtractor(extractor: EventAttributesExtractor<CrashDetails>) {
        additionalExtractors.add(extractor)
    }

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        addAttributesExtractor(RuntimeDetailsExtractor.create(context))
        val reporter = CrashReporter(additionalExtractors)
        reporter.install(openTelemetryRum.openTelemetry)
        crashReporter = reporter
    }

    override fun uninstall(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        crashReporter?.uninstall()
        crashReporter = null
    }

    override val name: String = "java-crash"
}
