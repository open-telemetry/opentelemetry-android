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

    /** Adds an [EventAttributesExtractor] that will extract additional attributes.  */
    fun addAttributesExtractor(extractor: EventAttributesExtractor<CrashDetails>) {
        additionalExtractors.add(extractor)
    }

    override fun install(ctx: InstallationContext) {
        addAttributesExtractor(RuntimeDetailsExtractor.create<CrashDetails, Any>(ctx.application))
        val crashReporter = CrashReporter(additionalExtractors)
        crashReporter.install(ctx.openTelemetry as OpenTelemetrySdk)
    }

    override val name: String = "crash"
}
