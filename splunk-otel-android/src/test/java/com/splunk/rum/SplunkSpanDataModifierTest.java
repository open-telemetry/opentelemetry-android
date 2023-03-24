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

import static com.splunk.rum.SplunkSpanDataModifier.REACT_NATIVE_SPAN_ID_KEY;
import static com.splunk.rum.SplunkSpanDataModifier.REACT_NATIVE_TRACE_ID_KEY;
import static com.splunk.rum.SplunkSpanDataModifier.SPLUNK_OPERATION_KEY;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.rum.internal.RumConstants;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.testing.trace.TestSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SplunkSpanDataModifierTest {

    @Mock private SpanExporter delegate;
    @Captor private ArgumentCaptor<Collection<SpanData>> exportedSpansCaptor;

    @Test
    void changesSpanIdAttrName() {
        String sessionId = "abc123fonzie";
        Attributes attrs = Attributes.of(RumConstants.SESSION_ID_KEY, sessionId);
        SpanData original = startBuilder().setAttributes(attrs).build();

        CompletableResultCode exportResult = CompletableResultCode.ofSuccess();
        when(delegate.export(exportedSpansCaptor.capture())).thenReturn(exportResult);

        SplunkSpanDataModifier underTest = new SplunkSpanDataModifier(delegate, false);
        underTest.export(singletonList(original));

        Collection<SpanData> exported = exportedSpansCaptor.getValue();
        assertThat(exported).hasSize(1);
        SpanData first = exported.iterator().next();
        assertThat(first.getAttributes().get(StandardAttributes.SESSION_ID_KEY))
                .isEqualTo(sessionId);
        assertThat(first.getAttributes().get(RumConstants.SESSION_ID_KEY)).isEqualTo(sessionId);
    }

    @Test
    void shouldConvertExceptionEventsToSpanAttributes() {
        SpanData original =
                startBuilder()
                        .setEvents(
                                Arrays.asList(
                                        EventData.create(
                                                123,
                                                "test",
                                                Attributes.of(stringKey("attribute"), "value")),
                                        EventData.create(
                                                456,
                                                SemanticAttributes.EXCEPTION_EVENT_NAME,
                                                Attributes.builder()
                                                        .put(
                                                                SemanticAttributes.EXCEPTION_TYPE,
                                                                "com.example.Error")
                                                        .put(
                                                                SemanticAttributes
                                                                        .EXCEPTION_MESSAGE,
                                                                "failed")
                                                        .put(
                                                                SemanticAttributes
                                                                        .EXCEPTION_STACKTRACE,
                                                                "<stacktrace>")
                                                        .build())))
                        .setAttributes(Attributes.of(stringKey("attribute"), "value"))
                        .build();

        CompletableResultCode exportResult = CompletableResultCode.ofSuccess();
        when(delegate.export(exportedSpansCaptor.capture())).thenReturn(exportResult);

        SpanExporter underTest = new SplunkSpanDataModifier(delegate, false);
        CompletableResultCode actual = underTest.export(singleton(original));

        assertThat(actual).isSameAs(exportResult);

        Collection<SpanData> exportedSpans = exportedSpansCaptor.getValue();
        assertThat(exportedSpans).hasSize(1);
        assertThat(exportedSpans.iterator().next())
                .hasName("test")
                .hasKind(SpanKind.CLIENT)
                .hasEvents(
                        EventData.create(
                                123, "test", Attributes.of(stringKey("attribute"), "value")))
                .hasTotalRecordedEvents(1)
                .hasAttributesSatisfyingExactly(
                        equalTo(SPLUNK_OPERATION_KEY, "test"),
                        equalTo(stringKey("attribute"), "value"),
                        equalTo(SemanticAttributes.EXCEPTION_TYPE, "Error"),
                        equalTo(SplunkRum.ERROR_TYPE_KEY, "Error"),
                        equalTo(SemanticAttributes.EXCEPTION_MESSAGE, "failed"),
                        equalTo(SplunkRum.ERROR_MESSAGE_KEY, "failed"),
                        equalTo(SemanticAttributes.EXCEPTION_STACKTRACE, "<stacktrace>"))
                .hasTotalAttributeCount(7);
    }

    @Test
    void shouldSetCaseSensitiveSpanNameToAttribute() {
        SpanData original = startBuilder("SplunkRumSpan").build();

        CompletableResultCode exportResult = CompletableResultCode.ofSuccess();
        when(delegate.export(exportedSpansCaptor.capture())).thenReturn(exportResult);

        SpanExporter underTest = new SplunkSpanDataModifier(delegate, false);
        CompletableResultCode actual = underTest.export(singleton(original));

        assertThat(actual).isSameAs(exportResult);

        Collection<SpanData> exportedSpans = exportedSpansCaptor.getValue();
        assertThat(exportedSpans).hasSize(1);
        assertThat(exportedSpans.iterator().next())
                .hasName("SplunkRumSpan")
                .hasAttributesSatisfyingExactly(equalTo(SPLUNK_OPERATION_KEY, "SplunkRumSpan"));
    }

    @Test
    void shouldCopySelectedResourceAttributes() {
        Resource resource =
                Resource.create(
                        Attributes.builder()
                                .put("custom.ignored.attribute", "something")
                                .put(ResourceAttributes.SERVICE_NAME, "myApp")
                                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, "dev")
                                .put(ResourceAttributes.DEVICE_MODEL_NAME, "phone")
                                .put(ResourceAttributes.DEVICE_MODEL_IDENTIFIER, "phone 12345")
                                .put(ResourceAttributes.OS_NAME, "Android")
                                .put(ResourceAttributes.OS_TYPE, "linux")
                                .put(ResourceAttributes.OS_VERSION, "13")
                                .put(SplunkRum.APP_NAME_KEY, "myApp")
                                .put(SplunkRum.RUM_VERSION_KEY, "1.0.0")
                                .build());

        SpanData original =
                TestSpanData.builder()
                        .setName("SplunkRumSpan")
                        .setKind(SpanKind.CLIENT)
                        .setStatus(StatusData.unset())
                        .setStartEpochNanos(12345)
                        .setEndEpochNanos(67890)
                        .setHasEnded(true)
                        .setResource(resource)
                        .build();

        CompletableResultCode exportResult = CompletableResultCode.ofSuccess();
        when(delegate.export(exportedSpansCaptor.capture())).thenReturn(exportResult);

        SpanExporter underTest = new SplunkSpanDataModifier(delegate, false);
        CompletableResultCode actual = underTest.export(singleton(original));

        assertThat(actual).isSameAs(exportResult);

        Collection<SpanData> exportedSpans = exportedSpansCaptor.getValue();
        assertThat(exportedSpans).hasSize(1);
        assertThat(exportedSpans.iterator().next())
                .hasName("SplunkRumSpan")
                .hasAttributesSatisfyingExactly(
                        equalTo(SPLUNK_OPERATION_KEY, "SplunkRumSpan"),
                        equalTo(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, "dev"),
                        equalTo(ResourceAttributes.DEVICE_MODEL_NAME, "phone"),
                        equalTo(ResourceAttributes.DEVICE_MODEL_IDENTIFIER, "phone 12345"),
                        equalTo(ResourceAttributes.OS_NAME, "Android"),
                        equalTo(ResourceAttributes.OS_TYPE, "linux"),
                        equalTo(ResourceAttributes.OS_VERSION, "13"),
                        equalTo(SplunkRum.APP_NAME_KEY, "myApp"),
                        equalTo(SplunkRum.RUM_VERSION_KEY, "1.0.0"));
    }

    @Test
    void shouldIgnoreReactIdsIfReactNativeSupportIsDisabled() {
        SpanContext spanContext =
                SpanContext.create(
                        "00000000000000000000000000000123",
                        "0000000000000456",
                        TraceFlags.getSampled(),
                        TraceState.getDefault());

        SpanData original =
                startBuilder("SplunkRumSpan")
                        .setSpanContext(spanContext)
                        .setAttributes(
                                Attributes.builder()
                                        .put(
                                                REACT_NATIVE_TRACE_ID_KEY,
                                                "99999999999999999999999999999999")
                                        .put(REACT_NATIVE_SPAN_ID_KEY, "8888888888888888")
                                        .build())
                        .build();

        CompletableResultCode exportResult = CompletableResultCode.ofSuccess();
        when(delegate.export(exportedSpansCaptor.capture())).thenReturn(exportResult);

        SpanExporter underTest = new SplunkSpanDataModifier(delegate, false);
        CompletableResultCode actual = underTest.export(singleton(original));

        assertThat(actual).isSameAs(exportResult);

        Collection<SpanData> exportedSpans = exportedSpansCaptor.getValue();
        assertThat(exportedSpans).hasSize(1);
        assertThat(exportedSpans.iterator().next())
                .hasName("SplunkRumSpan")
                .hasTraceId(spanContext.getTraceId())
                .hasSpanId(spanContext.getSpanId())
                .hasAttributesSatisfyingExactly(
                        equalTo(SPLUNK_OPERATION_KEY, "SplunkRumSpan"),
                        equalTo(REACT_NATIVE_TRACE_ID_KEY, "99999999999999999999999999999999"),
                        equalTo(REACT_NATIVE_SPAN_ID_KEY, "8888888888888888"));
    }

    @Test
    void shouldReplaceTraceAndSpanIdWithReactNativeIds() {
        SpanContext spanContext =
                SpanContext.create(
                        "00000000000000000000000000000123",
                        "0000000000000456",
                        TraceFlags.getSampled(),
                        TraceState.getDefault());

        SpanData original =
                TestSpanData.builder()
                        .setSpanContext(spanContext)
                        .setName("SplunkRumSpan")
                        .setKind(SpanKind.CLIENT)
                        .setStatus(StatusData.unset())
                        .setStartEpochNanos(12345)
                        .setEndEpochNanos(67890)
                        .setHasEnded(true)
                        .setAttributes(
                                Attributes.builder()
                                        .put(
                                                REACT_NATIVE_TRACE_ID_KEY,
                                                "99999999999999999999999999999999")
                                        .put(REACT_NATIVE_SPAN_ID_KEY, "8888888888888888")
                                        .build())
                        .build();

        CompletableResultCode exportResult = CompletableResultCode.ofSuccess();
        when(delegate.export(exportedSpansCaptor.capture())).thenReturn(exportResult);

        SpanExporter underTest = new SplunkSpanDataModifier(delegate, true);
        CompletableResultCode actual = underTest.export(singleton(original));

        assertThat(actual).isSameAs(exportResult);

        Collection<SpanData> exportedSpans = exportedSpansCaptor.getValue();
        assertThat(exportedSpans).hasSize(1);
        assertThat(exportedSpans.iterator().next())
                .hasName("SplunkRumSpan")
                .hasTraceId("99999999999999999999999999999999")
                .hasSpanId("8888888888888888")
                .hasAttributesSatisfyingExactly(equalTo(SPLUNK_OPERATION_KEY, "SplunkRumSpan"));
    }

    private TestSpanData.Builder startBuilder() {
        return startBuilder("test");
    }

    private TestSpanData.Builder startBuilder(String name) {
        return TestSpanData.builder()
                .setName(name)
                .setKind(SpanKind.CLIENT)
                .setStatus(StatusData.unset())
                .setStartEpochNanos(12345)
                .setEndEpochNanos(67890)
                .setHasEnded(true);
    }
}
