/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
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
    void integrationTest() throws InterruptedException {
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
        crashingThread.join();

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
