/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr

import android.os.Looper
import androidx.annotation.VisibleForTesting
import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor
import io.opentelemetry.android.internal.services.Services.Companion.get
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/** Entry point for [AnrDetector].  */
@AutoService(AndroidInstrumentation::class)
class AnrInstrumentation : AndroidInstrumentation {
    private val additionalExtractors: MutableList<EventAttributesExtractor<Array<StackTraceElement>>> =
        mutableListOf()
    private var mainLooper: Looper = Looper.getMainLooper()
    private var scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)

    /** Adds an [EventAttributesExtractor] that will extract additional attributes.  */
    fun addAttributesExtractor(extractor: EventAttributesExtractor<Array<StackTraceElement>>): AnrInstrumentation {
        additionalExtractors.add(extractor)
        return this
    }

    /** Sets a custom [Looper] to run on. Useful for testing.  */
    @VisibleForTesting
    fun setMainLooper(looper: Looper): AnrInstrumentation {
        mainLooper = looper
        return this
    }

    @VisibleForTesting
    internal fun setScheduler(scheduler: ScheduledExecutorService): AnrInstrumentation {
        this.scheduler = scheduler
        return this
    }

    override fun install(ctx: InstallationContext) {
        val anrDetector =
            AnrDetector(
                additionalExtractors,
                mainLooper,
                scheduler,
                get(ctx.context).appLifecycle,
                ctx.openTelemetry,
                ctx.sessionProvider,
            )
        anrDetector.start()
    }

    override val name: String = "anr"
}
