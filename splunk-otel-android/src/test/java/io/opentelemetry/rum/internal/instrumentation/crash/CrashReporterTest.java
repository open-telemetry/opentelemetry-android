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

package io.opentelemetry.rum.internal.instrumentation.crash;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.rum.internal.instrumentation.InstrumentedApplication;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.time.Duration;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CrashReporterTest {

    @RegisterExtension
    static final OpenTelemetryExtension testing = OpenTelemetryExtension.create();

    static Thread.UncaughtExceptionHandler existingHandler;

    @BeforeAll
    static void setUp() {
        existingHandler = Thread.getDefaultUncaughtExceptionHandler();
        // disable the handler installed by junit
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    @AfterAll
    static void tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(existingHandler);
    }

    @Test
    void integrationTest() {
        InstrumentedApplication instrumentedApplication = mock(InstrumentedApplication.class);
        when(instrumentedApplication.getOpenTelemetrySdk())
                .thenReturn((OpenTelemetrySdk) testing.getOpenTelemetry());

        CrashReporter.builder()
                .addAttributesExtractor(constant(stringKey("test.key"), "abc"))
                .build()
                .installOn(instrumentedApplication);

        RuntimeException crash = new RuntimeException("boooom!");
        Thread crashingThread =
                new Thread(
                        () -> {
                            throw crash;
                        });
        crashingThread.setDaemon(true);
        crashingThread.start();

        Attributes expectedAttributes =
                Attributes.builder()
                        .put(SemanticAttributes.EXCEPTION_ESCAPED, true)
                        .put(SemanticAttributes.THREAD_ID, crashingThread.getId())
                        .put(SemanticAttributes.THREAD_NAME, crashingThread.getName())
                        .put(stringKey("test.key"), "abc")
                        .build();
        assertTrace(
                trace ->
                        trace.hasSpansSatisfyingExactly(
                                span ->
                                        span.hasName("RuntimeException")
                                                .hasKind(SpanKind.INTERNAL)
                                                .hasStatus(StatusData.error())
                                                .hasException(crash)
                                                .hasAttributes(expectedAttributes)));
    }

    private static void assertTrace(Consumer<TraceAssert> assertion) {
        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> testing.assertTraces().hasTracesSatisfyingExactly(assertion));
    }
}
