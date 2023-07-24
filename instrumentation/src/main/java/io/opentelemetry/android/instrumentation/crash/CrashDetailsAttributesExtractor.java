/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

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
