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

import android.app.Application;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SplunkRumTest {

    @Rule
    public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();
    private Tracer tracer;
    private final Config config = Config.builder().realm("us0").rumAccessToken("token").applicationName("appName").build();

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        SplunkRum.resetSingletonForTest();
    }

    @Test
    public void initialization_onlyOnce() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        Config config = mock(Config.class);
        ConnectionUtil connectionUtil = mock(ConnectionUtil.class, RETURNS_DEEP_STUBS);

        when(config.getBeaconEndpoint()).thenReturn("http://backend");
        when(config.isDebugEnabled()).thenReturn(true);

        SplunkRum singleton = SplunkRum.initialize(config, application, () -> connectionUtil);
        SplunkRum sameInstance = SplunkRum.initialize(config, application);

        assertSame(singleton, sameInstance);
    }

    @Test
    public void getInstance_preConfig() {
        SplunkRum instance = SplunkRum.getInstance();
        assertTrue(instance instanceof NoOpSplunkRum);
    }

    @Test
    public void getInstance() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        Config config = mock(Config.class);

        when(config.getBeaconEndpoint()).thenReturn("http://backend");

        SplunkRum singleton = SplunkRum.initialize(config, application, () -> mock(ConnectionUtil.class, RETURNS_DEEP_STUBS));
        assertSame(singleton, SplunkRum.getInstance());
    }

    @Test
    public void newConfigBuilder() {
        assertNotNull(SplunkRum.newConfigBuilder());
    }

    @Test
    public void nonNullMethods() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        Config config = mock(Config.class);

        when(config.getBeaconEndpoint()).thenReturn("http://backend");

        SplunkRum splunkRum = SplunkRum.initialize(config, application, () -> mock(ConnectionUtil.class, RETURNS_DEEP_STUBS));
        assertNotNull(splunkRum.getOpenTelemetry());
        assertNotNull(splunkRum.getRumSessionId());
    }

    @Test
    public void addEvent() {
        SplunkRum splunkRum = new SplunkRum((OpenTelemetrySdk) otelTesting.getOpenTelemetry(), new SessionId(), config);

        Attributes attributes = Attributes.of(stringKey("one"), "1", longKey("two"), 2L);
        splunkRum.addRumEvent("foo", attributes);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertEquals("foo", spans.get(0).getName());
        assertEquals(attributes.asMap(), spans.get(0).getAttributes().asMap());
    }

    @Test
    public void updateGlobalAttributes() {
        SplunkRum splunkRum = new SplunkRum((OpenTelemetrySdk) otelTesting.getOpenTelemetry(), new SessionId(), config);

        splunkRum.updateGlobalAttributes(attributesBuilder -> attributesBuilder.put("key", "value"));
        splunkRum.setGlobalAttribute(longKey("otherKey"), 1234L);

        assertEquals(Attributes.of(stringKey("key"), "value", longKey("otherKey"), 1234L), config.getGlobalAttributes());
    }

    @Test
    public void recordAnr() {
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement stackTraceElement : stackTrace) {
            stringBuilder.append(stackTraceElement).append("\n");
        }

        SplunkRum splunkRum = new SplunkRum((OpenTelemetrySdk) otelTesting.getOpenTelemetry(), new SessionId(), config);

        Attributes expectedAttributes = Attributes.of(
                SemanticAttributes.EXCEPTION_STACKTRACE, stringBuilder.toString(),
                SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_ERROR);

        splunkRum.recordAnr(stackTrace);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertEquals("ANR", spans.get(0).getName());
        assertEquals(expectedAttributes.asMap(), spans.get(0).getAttributes().asMap());
    }

    @Test
    public void addException() {
        InMemorySpanExporter testExporter = InMemorySpanExporter.create();
        OpenTelemetrySdk testSdk = buildTestSdk(testExporter);

        SplunkRum splunkRum = new SplunkRum(testSdk, new SessionId(), config);

        Attributes attributes = Attributes.of(stringKey("one"), "1", longKey("two"), 2L);
        splunkRum.addRumException(new NullPointerException("oopsie"), attributes);

        List<SpanData> spans = testExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        assertEquals("NullPointerException", spans.get(0).getName());

        Attributes expected = attributes.toBuilder()
                .put(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_ERROR)
                .put(SemanticAttributes.EXCEPTION_MESSAGE, "oopsie")
                .put(SplunkRum.ERROR_MESSAGE_KEY, "oopsie")
                .put(SemanticAttributes.EXCEPTION_TYPE, "NullPointerException")
                .put(SplunkRum.ERROR_TYPE_KEY, "NullPointerException")
                .build();

        assertEquals(expected.asMap(), spans.get(0).getAttributes().asMap());
    }

    private OpenTelemetrySdk buildTestSdk(InMemorySpanExporter testExporter) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder()
                        .addSpanProcessor(SimpleSpanProcessor.create(testExporter))
                        .build())
                .build();
    }

    @Test
    public void createAndEnd() {
        SplunkRum splunkRum = new SplunkRum((OpenTelemetrySdk) otelTesting.getOpenTelemetry(), new SessionId(), config);

        Span span = splunkRum.startWorkflow("workflow");
        Span inner = tracer.spanBuilder("foo").startSpan();
        try (Scope scope = inner.makeCurrent()) {
            //do nothing
        } finally {
            inner.end();
        }
        span.end();

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());
        //verify we're not trying to do any propagation of the context here.
        assertEquals(spans.get(0).getParentSpanId(), SpanId.getInvalid());
        assertEquals("foo", spans.get(0).getName());
        assertEquals("workflow", spans.get(1).getName());
        assertEquals("workflow", spans.get(1).getAttributes().get(SplunkRum.WORKFLOW_NAME_KEY));
    }
}