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

package io.opentelemetry.rum.internal.instrumentation.crash;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

final class CrashDetailsAttributesExtractor implements AttributesExtractor<CrashDetails, Void> {

    @Override
    public void onStart(
            AttributesBuilder attributes, Context parentContext, CrashDetails crashDetails) {
        attributes.put(SemanticAttributes.THREAD_ID, crashDetails.getThread().getId());
        attributes.put(SemanticAttributes.THREAD_NAME, crashDetails.getThread().getName());
        attributes.put(SemanticAttributes.EXCEPTION_ESCAPED, true);
    }

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            CrashDetails crashDetails,
            Void unused,
            Throwable error) {}
}
