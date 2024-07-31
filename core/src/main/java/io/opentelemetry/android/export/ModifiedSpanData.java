/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;

final class ModifiedSpanData extends DelegatingSpanData {

    private final Attributes modifiedAttributes;

    ModifiedSpanData(SpanData original, Attributes modifiedAttributes) {
        super(original);
        this.modifiedAttributes = modifiedAttributes;
    }

    @Override
    public Attributes getAttributes() {
        return modifiedAttributes;
    }

    @Override
    public int getTotalAttributeCount() {
        return modifiedAttributes.size();
    }
}
