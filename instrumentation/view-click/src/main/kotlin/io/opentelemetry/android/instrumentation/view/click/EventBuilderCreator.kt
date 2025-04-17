/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click
import android.view.View
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.incubator.logs.ExtendedLogRecordBuilder
import io.opentelemetry.api.incubator.logs.ExtendedLogger

object EventBuilderCreator {
    private var eventLogger =
        OpenTelemetry
            .noop()
            .logsBridge
            .loggerBuilder("io.opentelemetry.android.view.click.noop")
            .build() as ExtendedLogger

    @JvmStatic
    fun configure(context: InstallationContext) {
        eventLogger =
            context.openTelemetry
                .logsBridge
                .loggerBuilder("io.opentelemetry.android.view.click")
                .build() as ExtendedLogger
    }

    @JvmStatic
    fun createEvent(name: String): ExtendedLogRecordBuilder =
        eventLogger
            .logRecordBuilder()
            .setEventName(name)

    @JvmStatic
    fun createViewAttributes(view: View): Attributes {
        val builder = Attributes.builder()
        builder.put(viewNameAttr, viewToName(view))
        builder.put(viewIdAttr, view.id.toLong())

        builder.put(xCoordinateAttr, view.x.toDouble())
        builder.put(yCoordinateAttr, view.y.toDouble())
        return builder.build()
    }

    @JvmStatic
    fun viewToName(view: View): String =
        try {
            view.resources?.getResourceEntryName(view.id) ?: view.id.toString()
        } catch (throwable: Throwable) {
            view.id.toString()
        }
}
