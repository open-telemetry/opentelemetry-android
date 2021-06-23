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

package com.splunk.rum;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ThrottlingExporterTest {
    @Mock
    private SpanExporter delegate;

    @Test
    public void shouldExportAllSpansBelowLimit() {
        // given
        SpanExporter underTest = ThrottlingExporter.newBuilder(delegate)
                .maxSpansInWindow(3)
                .windowSize(Duration.ofSeconds(15))
                .build();

        Instant now = Instant.ofEpochMilli(10_000);

        List<SpanData> spanData = asList(
                span("ui", now),
                span("ui", now.plus(5, ChronoUnit.SECONDS)),
                span("ui", now.plus(10, ChronoUnit.SECONDS)),
                span("ui", now.plus(15, ChronoUnit.SECONDS)),
                span("ui", now.plus(20, ChronoUnit.SECONDS))
        );

        // when
        underTest.export(spanData);

        // then
        verify(delegate).export(spanData);
    }

    @Test
    public void shouldThrottleSpansOverLimit() {
        // given
        SpanExporter underTest = ThrottlingExporter.newBuilder(delegate)
                .maxSpansInWindow(2)
                .windowSize(Duration.ofSeconds(15))
                .build();

        Instant now = Instant.ofEpochMilli(10_000);

        List<SpanData> spanData = asList(
                span("ui", now),
                span("ui", now.plus(5, ChronoUnit.SECONDS)),
                span("ui", now.plus(10, ChronoUnit.SECONDS)),
                span("ui", now.plus(15, ChronoUnit.SECONDS)),
                span("ui", now.plus(20, ChronoUnit.SECONDS)),
                span("ui", now.plus(25, ChronoUnit.SECONDS))
        );

        // when
        underTest.export(spanData);

        // then
        verify(delegate).export(asList(
                spanData.get(0),
                spanData.get(1),
                // idx=2 will be skipped because it's the 3rd span in the last 15 secs
                spanData.get(3),
                spanData.get(4)
                // idx=5 will be skipped because it's the 3rd span in the last 15 secs
        ));
    }

    @Test
    public void shouldCountDifferentComponentsSeparately() {
        // given
        SpanExporter underTest = ThrottlingExporter.newBuilder(delegate)
                .categorizeByAttribute(SplunkRum.COMPONENT_KEY)
                .maxSpansInWindow(2)
                .windowSize(Duration.ofSeconds(15))
                .build();

        Instant now = Instant.ofEpochMilli(10_000);

        List<SpanData> spanData = asList(
                span("ui", now),
                span("error", now.plus(5, ChronoUnit.SECONDS)),
                span("ui", now.plus(10, ChronoUnit.SECONDS)),
                span("ui", now.plus(15, ChronoUnit.SECONDS)),
                span("ui", now.plus(20, ChronoUnit.SECONDS)),
                span("error", now.plus(25, ChronoUnit.SECONDS)),
                //user-generated spans probably won't have a "component" attribute at all.
                span(null, now.plus(30, ChronoUnit.SECONDS)),
                span(null, now.plus(35, ChronoUnit.SECONDS)),
                span(null, now.plus(40, ChronoUnit.SECONDS)),
                span(null, now.plus(45, ChronoUnit.SECONDS))
        );

        // when
        underTest.export(spanData);

        // then
        verify(delegate).export(asList(
                spanData.get(0),
                spanData.get(1),
                spanData.get(2),
                spanData.get(3),
                // idx=4 will be skipped because it's the 3rd component=ui span in the last 15 secs
                spanData.get(5),
                spanData.get(6),
                spanData.get(7),
                // idx = 8 will be skipped because it's the 3rd no-component span in the 2-span, 15s window
                spanData.get(9)
        ));
    }

    @Test
    public void shouldKeepStateBetweenExportCalls() {
        // given
        SpanExporter underTest = ThrottlingExporter.newBuilder(delegate)
                .categorizeByAttribute(SplunkRum.COMPONENT_KEY)
                .maxSpansInWindow(2)
                .windowSize(Duration.ofSeconds(15))
                .build();

        Instant now = Instant.ofEpochMilli(10_000);

        // when
        List<SpanData> spanData = asList(
                span("appstart", now),
                span("ui", now.plus(5, ChronoUnit.SECONDS)),
                span("error", now.plus(6, ChronoUnit.SECONDS)),
                span("ui", now.plus(10, ChronoUnit.SECONDS))
        );
        underTest.export(spanData);

        // then
        verify(delegate).export(spanData);

        // when
        spanData = asList(
                span("ui", now.plus(15, ChronoUnit.SECONDS)),
                span("error", now.plus(16, ChronoUnit.SECONDS)),
                span("error", now.plus(20, ChronoUnit.SECONDS)),
                span("ui", now.plus(20, ChronoUnit.SECONDS))
        );
        underTest.export(spanData);

        // then
        verify(delegate).export(asList(
                // idx=0 will be skipped because it's the 3rd component=ui span in the last 15 secs
                spanData.get(1),
                // idx=2 will be skipped because it's the 3rd component=error span in the last 15 secs
                spanData.get(3)
        ));
    }

    @Test
    public void shouldDelegateFlushCall() {
        // given
        SpanExporter underTest = ThrottlingExporter.newBuilder(delegate).build();

        // when
        underTest.flush();

        // then
        verify(delegate).flush();
    }

    @Test
    public void shouldDelegateShutdownCall() {
        // given
        SpanExporter underTest = ThrottlingExporter.newBuilder(delegate).build();

        // when
        underTest.shutdown();

        // then
        verify(delegate).shutdown();
    }

    private static SpanData span(String component, Instant endTime) {
        return TestSpanData.builder()
                .setName("test")
                .setKind(SpanKind.INTERNAL)
                .setStatus(StatusData.unset())
                .setHasEnded(true)
                .setStartEpochNanos(0)
                .setEndEpochNanos(TimeUnit.SECONDS.toNanos(endTime.getEpochSecond()) + endTime.getNano())
                .setAttributes(Attributes.of(SplunkRum.COMPONENT_KEY, component))
                .build();
    }

}
