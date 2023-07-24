/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal.export;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.rum.internal.export.TestSpanHelper.span;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FilteringSpanExporterTest {

    @Captor ArgumentCaptor<Collection<SpanData>> spansCaptor;

    @Test
    void filter() {
        SpanData span1 = span("one");
        SpanData span2 = span("two");
        SpanData span3 = span("three");
        SpanData span4 = span("four");
        SpanData span5 = span("FIVE");
        Attributes attr6 = Attributes.of(stringKey("herp"), "derp");
        SpanData span6 = span("six", attr6);
        Attributes attr7 = Attributes.of(stringKey("dig"), "dug");
        SpanData span7 = span("seven", attr7);
        SpanData span8 = span("eight");
        Collection<SpanData> spans =
                Arrays.asList(span1, span2, span3, span4, span5, span6, span7, span8);
        Map<AttributeKey<?>, Predicate<?>> attrRejects = new HashMap<>();
        attrRejects.put(stringKey("herp"), "derp"::equals);
        attrRejects.put(stringKey("dig"), v -> ((String) v).startsWith("d"));

        SpanExporter exporter = mock(SpanExporter.class);
        CompletableResultCode expectedResult = mock(CompletableResultCode.class);

        when(exporter.export(spansCaptor.capture())).thenReturn(expectedResult);

        SpanExporter underTest =
                FilteringSpanExporter.builder(exporter)
                        .rejecting(x -> x == span2)
                        .rejectSpansWithNameContaining("hree")
                        .rejectSpansNamed("four")
                        .rejectSpansNamed(x -> x.equalsIgnoreCase("five"))
                        .rejectSpansWithAttributesMatching(attrRejects)
                        .build();

        CompletableResultCode result = underTest.export(spans);
        assertThat(result).isSameAs(expectedResult);
        Collection<SpanData> resultSpans = spansCaptor.getValue();
        assertThat(resultSpans)
                .satisfiesExactly(
                        s -> assertThat(s).isSameAs(span1), s -> assertThat(s).isSameAs(span8));
    }
}
