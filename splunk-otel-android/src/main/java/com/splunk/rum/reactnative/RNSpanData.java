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

package com.splunk.rum.reactnative;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.internal.ImmutableSpanContext;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;

public final class RNSpanData extends DelegatingSpanData {

    private final SpanContext modifiedContext;
    private final Attributes modifiedAttributes;

    static SpanData create(SpanData original) {
        SpanContext originalSpanContext = original.getSpanContext();
        Attributes attributes = original.getAttributes();
        AttributeKey<String> traceIdKey = AttributeKey.stringKey("_reactnative_traceId");
        AttributeKey<String> spanIdKey = AttributeKey.stringKey("_reactnative_spanId");

        String traceIdFromRN = attributes.get(traceIdKey);
        String spanIdFromRN = attributes.get(spanIdKey);
        if (traceIdFromRN == null || spanIdFromRN == null) {
            return original;
        }

        Attributes attributesWithoutRNIds =
                attributes.toBuilder().remove(traceIdKey).remove(spanIdKey).build();

        SpanContext rnContext =
                ImmutableSpanContext.create(
                        traceIdFromRN,
                        spanIdFromRN,
                        originalSpanContext.getTraceFlags(),
                        originalSpanContext.getTraceState(),
                        originalSpanContext.isRemote(),
                        false);
        return new RNSpanData(original, rnContext, attributesWithoutRNIds);
    }

    private RNSpanData(
            SpanData delegate, SpanContext modifiedContext, Attributes modifiedAttributes) {
        super(delegate);
        this.modifiedContext = modifiedContext;
        this.modifiedAttributes = modifiedAttributes;
    }

    @Override
    public SpanContext getSpanContext() {
        return this.modifiedContext;
    }

    @Override
    public Attributes getAttributes() {
        return this.modifiedAttributes;
    }
}
