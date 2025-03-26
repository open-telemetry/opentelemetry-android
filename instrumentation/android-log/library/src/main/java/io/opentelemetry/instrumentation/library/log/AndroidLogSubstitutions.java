/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.log;

import static io.opentelemetry.instrumentation.library.log.internal.LogRecordBuilderCreator.createLogRecordBuilder;
import static io.opentelemetry.instrumentation.library.log.internal.LogRecordBuilderCreator.getEventName;
import static io.opentelemetry.instrumentation.library.log.internal.LogRecordBuilderCreator.printStacktrace;

import android.util.Log;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;

public class AndroidLogSubstitutions {

    public static String TAG_KEY = "android.tag";

    public static String MESSAGE_KEY = "android.message";

    public static int substitutionForVerbose(String tag, String message) {
        createLogRecordBuilder()
                .setSeverity(Severity.TRACE)
                .setAllAttributes(Attributes.builder().put(TAG_KEY, tag).build())
                .setBody(message)
                .emit();

        return Log.v(tag, message);
    }

    public static int substitutionForVerbose2(String tag, String message, Throwable throwable) {
        createLogRecordBuilder()
                .setEventName(getEventName(throwable))
                .setSeverity(Severity.TRACE)
                .setAllAttributes(
                        Attributes.builder().put(TAG_KEY, tag).put(MESSAGE_KEY, message).build())
                .setBody(printStacktrace(throwable))
                .emit();

        return Log.v(tag, message, throwable);
    }

    public static int substitutionForDebug(String tag, String message) {
        createLogRecordBuilder()
                .setSeverity(Severity.DEBUG)
                .setAllAttributes(Attributes.builder().put(TAG_KEY, tag).build())
                .setBody(message)
                .emit();

        return Log.d(tag, message);
    }

    public static int substitutionForDebug2(String tag, String message, Throwable throwable) {
        createLogRecordBuilder()
                .setEventName(getEventName(throwable))
                .setSeverity(Severity.DEBUG)
                .setAllAttributes(
                        Attributes.builder().put(TAG_KEY, tag).put(MESSAGE_KEY, message).build())
                .setBody(printStacktrace(throwable))
                .emit();

        return Log.d(tag, message, throwable);
    }

    public static int substitutionForInfo(String tag, String message) {
        createLogRecordBuilder()
                .setSeverity(Severity.INFO)
                .setAllAttributes(Attributes.builder().put(TAG_KEY, tag).build())
                .setBody(message)
                .emit();

        return Log.i(tag, message);
    }

    public static int substitutionForInfo2(String tag, String message, Throwable throwable) {
        createLogRecordBuilder()
                .setEventName(getEventName(throwable))
                .setSeverity(Severity.INFO)
                .setAllAttributes(
                        Attributes.builder().put(TAG_KEY, tag).put(MESSAGE_KEY, message).build())
                .setBody(printStacktrace(throwable))
                .emit();

        return Log.i(tag, message, throwable);
    }

    public static int substitutionForWarn(String tag, String message) {
        createLogRecordBuilder()
                .setSeverity(Severity.WARN)
                .setAllAttributes(Attributes.builder().put(TAG_KEY, tag).build())
                .setBody(message)
                .emit();

        return Log.w(tag, message);
    }

    public static int substitutionForWarn2(String tag, Throwable throwable) {
        createLogRecordBuilder()
                .setEventName(getEventName(throwable))
                .setSeverity(Severity.WARN)
                .setAllAttributes(Attributes.builder().put(TAG_KEY, tag).build())
                .setBody(printStacktrace(throwable))
                .emit();

        return Log.w(tag, throwable);
    }

    public static int substitutionForWarn3(String tag, String message, Throwable throwable) {
        createLogRecordBuilder()
                .setEventName(getEventName(throwable))
                .setSeverity(Severity.WARN)
                .setAllAttributes(
                        Attributes.builder().put(TAG_KEY, tag).put(MESSAGE_KEY, message).build())
                .setBody(printStacktrace(throwable))
                .emit();

        return Log.w(tag, message, throwable);
    }

    public static int substitutionForError(String tag, String message) {
        createLogRecordBuilder()
                .setSeverity(Severity.ERROR)
                .setAllAttributes(Attributes.builder().put(TAG_KEY, tag).build())
                .setBody(message)
                .emit();

        return Log.e(tag, message);
    }

    public static int substitutionForError2(String tag, String message, Throwable throwable) {
        createLogRecordBuilder()
                .setEventName(getEventName(throwable))
                .setSeverity(Severity.ERROR)
                .setAllAttributes(
                        Attributes.builder().put(TAG_KEY, tag).put(MESSAGE_KEY, message).build())
                .setBody(printStacktrace(throwable))
                .emit();

        return Log.e(tag, message, throwable);
    }

    public static int substitutionForWtf(String tag, String message) {
        createLogRecordBuilder()
                .setSeverity(Severity.UNDEFINED_SEVERITY_NUMBER)
                .setAllAttributes(Attributes.builder().put(TAG_KEY, tag).build())
                .setBody(message)
                .emit();

        return Log.wtf(tag, message);
    }

    public static int substitutionForWtf2(String tag, Throwable throwable) {
        createLogRecordBuilder()
                .setEventName(getEventName(throwable))
                .setSeverity(Severity.UNDEFINED_SEVERITY_NUMBER)
                .setAllAttributes(Attributes.builder().put(TAG_KEY, tag).build())
                .setBody(printStacktrace(throwable))
                .emit();

        return Log.wtf(tag, throwable);
    }

    public static int substitutionForWtf3(String tag, String message, Throwable throwable) {
        createLogRecordBuilder()
                .setEventName(getEventName(throwable))
                .setSeverity(Severity.UNDEFINED_SEVERITY_NUMBER)
                .setAllAttributes(
                        Attributes.builder().put(TAG_KEY, tag).put(MESSAGE_KEY, message).build())
                .setBody(printStacktrace(throwable))
                .emit();

        return Log.wtf(tag, message, throwable);
    }
}
