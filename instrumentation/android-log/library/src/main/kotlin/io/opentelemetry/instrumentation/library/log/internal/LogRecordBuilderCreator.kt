/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.log.internal

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.logs.LogRecordBuilder

object LogRecordBuilderCreator {
    private var logger =
        OpenTelemetry
            .noop()
            .logsBridge
            .loggerBuilder("io.opentelemetry.android.log.noop")
            .build()

    @JvmStatic
    fun configure(openTelemetry: OpenTelemetry) {
        logger =
            openTelemetry
                .logsBridge
                .loggerBuilder("io.opentelemetry.android.log")
                .build()
    }

    @JvmStatic
    fun createLogRecordBuilder(): LogRecordBuilder = logger.logRecordBuilder()

    @JvmStatic
    fun printStacktrace(throwable: Throwable): String = throwable.stackTraceToString()

    @JvmStatic
    fun getTypeName(throwable: Throwable): String {
        var eventName = throwable.javaClass.canonicalName
        if (eventName == null) eventName = throwable.javaClass.simpleName
        return eventName!!
    }
}
