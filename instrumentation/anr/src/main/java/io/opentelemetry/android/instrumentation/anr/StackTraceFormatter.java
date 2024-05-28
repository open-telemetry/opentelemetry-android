/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

final class StackTraceFormatter implements AttributesExtractor<StackTraceElement[], Void> {

    @Override
    public void onStart(
            AttributesBuilder attributes, Context parentContext, StackTraceElement[] stackTrace) {
        StringBuilder stackTraceString = new StringBuilder();
        for (StackTraceElement stackTraceElement : stackTrace) {
            stackTraceString.append(stackTraceElement).append("\n");
        }
        attributes.put(EXCEPTION_STACKTRACE, stackTraceString.toString());
    }

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            StackTraceElement[] stackTraceElements,
            Void unused,
            Throwable error) {}
}
