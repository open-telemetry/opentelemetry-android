/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.log

import android.util.Log
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.instrumentation.library.log.internal.LogRecordBuilderCreator.createLogRecordBuilder
import io.opentelemetry.instrumentation.library.log.internal.LogRecordBuilderCreator.getTypeName
import io.opentelemetry.instrumentation.library.log.internal.LogRecordBuilderCreator.printStacktrace
import io.opentelemetry.semconv.ExceptionAttributes

object AndroidLogSubstitutions {
    val tagKey: AttributeKey<String> = AttributeKey.stringKey("android.log.tag")

    @JvmStatic
    fun substitutionForVerbose(
        tag: String?,
        message: String,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.TRACE)
            .setAllAttributes(Attributes.builder().put(tagKey, tag).build())
            .setBody(message)
            .emit()

        return Log.v(tag, message)
    }

    @JvmStatic
    fun substitutionForVerbose2(
        tag: String?,
        message: String,
        throwable: Throwable,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.TRACE)
            .setAllAttributes(
                Attributes
                    .builder()
                    .put(tagKey, tag)
                    .put(ExceptionAttributes.EXCEPTION_TYPE, getTypeName(throwable))
                    .put(ExceptionAttributes.EXCEPTION_STACKTRACE, printStacktrace(throwable))
                    .build(),
            ).setBody(message)
            .emit()

        return Log.v(tag, message, throwable)
    }

    @JvmStatic
    fun substitutionForDebug(
        tag: String?,
        message: String,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.DEBUG)
            .setAllAttributes(Attributes.builder().put(tagKey, tag).build())
            .setBody(message)
            .emit()

        return Log.d(tag, message)
    }

    @JvmStatic
    fun substitutionForDebug2(
        tag: String?,
        message: String,
        throwable: Throwable,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.DEBUG)
            .setAllAttributes(
                Attributes
                    .builder()
                    .put(tagKey, tag)
                    .put(ExceptionAttributes.EXCEPTION_TYPE, getTypeName(throwable))
                    .put(ExceptionAttributes.EXCEPTION_STACKTRACE, printStacktrace(throwable))
                    .build(),
            ).setBody(message)
            .emit()

        return Log.d(tag, message, throwable)
    }

    @JvmStatic
    fun substitutionForInfo(
        tag: String?,
        message: String,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.INFO)
            .setAllAttributes(Attributes.builder().put(tagKey, tag).build())
            .setBody(message)
            .emit()

        return Log.i(tag, message)
    }

    @JvmStatic
    fun substitutionForInfo2(
        tag: String?,
        message: String,
        throwable: Throwable,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.INFO)
            .setAllAttributes(
                Attributes
                    .builder()
                    .put(tagKey, tag)
                    .put(ExceptionAttributes.EXCEPTION_TYPE, getTypeName(throwable))
                    .put(ExceptionAttributes.EXCEPTION_STACKTRACE, printStacktrace(throwable))
                    .build(),
            ).setBody(message)
            .emit()

        return Log.i(tag, message, throwable)
    }

    @JvmStatic
    fun substitutionForWarn(
        tag: String?,
        message: String,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.WARN)
            .setAllAttributes(Attributes.builder().put(tagKey, tag).build())
            .setBody(message)
            .emit()

        return Log.w(tag, message)
    }

    @JvmStatic
    fun substitutionForWarn2(
        tag: String?,
        throwable: Throwable,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.WARN)
            .setAllAttributes(
                Attributes
                    .builder()
                    .put(tagKey, tag)
                    .put(ExceptionAttributes.EXCEPTION_TYPE, getTypeName(throwable))
                    .put(ExceptionAttributes.EXCEPTION_STACKTRACE, printStacktrace(throwable))
                    .build(),
            ).emit()

        return Log.w(tag, throwable)
    }

    @JvmStatic
    fun substitutionForWarn3(
        tag: String?,
        message: String,
        throwable: Throwable,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.WARN)
            .setAllAttributes(
                Attributes
                    .builder()
                    .put(tagKey, tag)
                    .put(ExceptionAttributes.EXCEPTION_TYPE, getTypeName(throwable))
                    .put(ExceptionAttributes.EXCEPTION_STACKTRACE, printStacktrace(throwable))
                    .build(),
            ).setBody(message)
            .emit()

        return Log.w(tag, message, throwable)
    }

    @JvmStatic
    fun substitutionForError(
        tag: String?,
        message: String,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.ERROR)
            .setAllAttributes(Attributes.builder().put(tagKey, tag).build())
            .setBody(message)
            .emit()

        return Log.e(tag, message)
    }

    @JvmStatic
    fun substitutionForError2(
        tag: String?,
        message: String,
        throwable: Throwable,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.ERROR)
            .setAllAttributes(
                Attributes
                    .builder()
                    .put(tagKey, tag)
                    .put(ExceptionAttributes.EXCEPTION_TYPE, getTypeName(throwable))
                    .put(ExceptionAttributes.EXCEPTION_STACKTRACE, printStacktrace(throwable))
                    .build(),
            ).setBody(message)
            .emit()

        return Log.e(tag, message, throwable)
    }

    @JvmStatic
    fun substitutionForWtf(
        tag: String?,
        message: String,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.UNDEFINED_SEVERITY_NUMBER)
            .setAllAttributes(Attributes.builder().put(tagKey, tag).build())
            .setBody(message)
            .emit()

        return Log.wtf(tag, message)
    }

    @JvmStatic
    fun substitutionForWtf2(
        tag: String?,
        throwable: Throwable,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.UNDEFINED_SEVERITY_NUMBER)
            .setAllAttributes(
                Attributes
                    .builder()
                    .put(tagKey, tag)
                    .put(ExceptionAttributes.EXCEPTION_TYPE, getTypeName(throwable))
                    .put(ExceptionAttributes.EXCEPTION_STACKTRACE, printStacktrace(throwable))
                    .build(),
            ).emit()

        return Log.wtf(tag, throwable)
    }

    @JvmStatic
    fun substitutionForWtf3(
        tag: String?,
        message: String,
        throwable: Throwable,
    ): Int {
        createLogRecordBuilder()
            .setSeverity(Severity.UNDEFINED_SEVERITY_NUMBER)
            .setAllAttributes(
                Attributes
                    .builder()
                    .put(tagKey, tag)
                    .put(ExceptionAttributes.EXCEPTION_TYPE, getTypeName(throwable))
                    .put(ExceptionAttributes.EXCEPTION_STACKTRACE, printStacktrace(throwable))
                    .build(),
            ).setBody(message)
            .emit()

        return Log.wtf(tag, message, throwable)
    }
}
