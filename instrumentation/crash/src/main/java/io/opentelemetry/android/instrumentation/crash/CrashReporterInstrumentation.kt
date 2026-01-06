/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.sdk.OpenTelemetrySdk

/** Entrypoint for installing the crash reporting instrumentation.  */
@AutoService(AndroidInstrumentation::class)
class CrashReporterInstrumentation : AndroidInstrumentation {
    private val additionalExtractors: MutableList<EventAttributesExtractor<CrashDetails>> =
        mutableListOf()
    private var crashReportingMode: CrashReportingMode = CrashReportingMode.LOGS_ONLY

    /** Adds an [EventAttributesExtractor] that will extract additional attributes.  */
    fun addAttributesExtractor(extractor: EventAttributesExtractor<CrashDetails>) {
        additionalExtractors.add(extractor)
    }

    /**
     * Sets the crash reporting mode to control how crashes are emitted.
     *
     * @param mode The [CrashReportingMode] to use. Defaults to [CrashReportingMode.LOGS_ONLY].
     * @return this for method chaining
     * @see CrashReportingMode
     */
    fun setCrashReportingMode(mode: CrashReportingMode): CrashReporterInstrumentation {
        this.crashReportingMode = mode
        return this
    }

    override fun install(ctx: InstallationContext) {
        addAttributesExtractor(RuntimeDetailsExtractor.create(ctx.context))
        val crashReporter = CrashReporter(additionalExtractors, crashReportingMode)

        // TODO avoid using OpenTelemetrySdk methods, only use the ones from OpenTelemetry api.
        crashReporter.install(ctx.openTelemetry as OpenTelemetrySdk)
    }

    override val name: String = "crash"
}
