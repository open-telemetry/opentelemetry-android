/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.screenorientation

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.instrumentation.screenorientation.model.Orientation

/**
 * An Android instrumentation module that installs and manages [ScreenOrientationDetector].
 *
 * Use [addAttributesExtractor] to attach custom extractors that enrich emitted orientation events.
 */
@AutoService(AndroidInstrumentation::class)
class ScreenOrientationInstrumentation : AndroidInstrumentation {
    private lateinit var detector: ScreenOrientationDetector

    private val additionalExtractors: MutableList<EventAttributesExtractor<Orientation>> =
        mutableListOf()

    /**
     * Adds an [EventAttributesExtractor] that will be used to extract additional
     * attributes from orientation events.
     *
     * @param extractor The extractor to add.
     * @return The current [ScreenOrientationInstrumentation] instance for chaining.
     */
    fun addAttributesExtractor(extractor: EventAttributesExtractor<Orientation>): ScreenOrientationInstrumentation {
        additionalExtractors.add(extractor)
        return this
    }

    override fun install(ctx: InstallationContext) {
        val logger =
            ctx.openTelemetry
                .logsBridge
                .loggerBuilder("io.opentelemetry.$name")
                .build()
        val applicationContext = ctx.context.applicationContext
        detector = ScreenOrientationDetector(applicationContext, logger, additionalExtractors)
        applicationContext.registerComponentCallbacks(detector)
    }

    override fun uninstall(ctx: InstallationContext) {
        if (!::detector.isInitialized) return

        ctx.context.applicationContext.unregisterComponentCallbacks(detector)
    }

    override val name: String = "screen_orientation"
}
