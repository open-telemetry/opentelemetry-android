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

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

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
