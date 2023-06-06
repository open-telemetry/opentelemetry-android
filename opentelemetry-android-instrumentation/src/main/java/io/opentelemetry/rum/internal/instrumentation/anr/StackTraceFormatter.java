/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.rum.internal.instrumentation.anr;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

final class StackTraceFormatter implements AttributesExtractor<StackTraceElement[], Void> {

    @Override
    public void onStart(
            AttributesBuilder attributes, Context parentContext, StackTraceElement[] stackTrace) {
        StringBuilder stackTraceString = new StringBuilder();
        for (StackTraceElement stackTraceElement : stackTrace) {
            stackTraceString.append(stackTraceElement).append("\n");
        }
        attributes.put(SemanticAttributes.EXCEPTION_STACKTRACE, stackTraceString.toString());
    }

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            StackTraceElement[] stackTraceElements,
            Void unused,
            Throwable error) {}
}
