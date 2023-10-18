/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.SemanticAttributes;
import org.junit.jupiter.api.Test;

class StackTraceFormatterTest {

    @Test
    void shouldSerializeStackTrace() {
        StackTraceElement[] stackTrace =
                new StackTraceElement[] {
                    new StackTraceElement("a.b.Class", "foo", "/src/a/b/Class.java", 42),
                    new StackTraceElement(
                            "a.b.AnotherClass", "bar", "/src/a/b/AnotherClass.java", 123)
                };
        StackTraceFormatter underTest = new StackTraceFormatter();

        AttributesBuilder startAttributes = Attributes.builder();
        underTest.onStart(startAttributes, Context.current(), stackTrace);
        assertThat(startAttributes.build())
                .hasSize(1)
                .containsEntry(
                        SemanticAttributes.EXCEPTION_STACKTRACE,
                        "a.b.Class.foo(/src/a/b/Class.java:42)\n"
                                + "a.b.AnotherClass.bar(/src/a/b/AnotherClass.java:123)\n");

        AttributesBuilder endAttributes = Attributes.builder();
        underTest.onEnd(endAttributes, Context.current(), stackTrace, null, null);
        assertThat(endAttributes.build()).isEmpty();
    }
}
