/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;

public class TestSpanHelper {

    static SpanData span(String name) {
        return span(name, Attributes.empty());
    }

    static SpanData span(String name, Attributes attributes) {
        return TestSpanData.builder()
                .setName(name)
                .setKind(SpanKind.INTERNAL)
                .setStatus(StatusData.unset())
                .setHasEnded(true)
                .setStartEpochNanos(0)
                .setEndEpochNanos(123)
                .setAttributes(attributes)
                .build();
    }
}
