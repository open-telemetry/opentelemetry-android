/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension;
import io.opentelemetry.semconv.ExceptionAttributes;
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes;
import java.util.List;
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
        OpenTelemetryRum openTelemetryRum = mock();
        when(openTelemetryRum.getOpenTelemetry())
                .thenReturn((OpenTelemetrySdk) testing.getOpenTelemetry());

        CrashReporterInstrumentation instrumentation = new CrashReporterInstrumentation();
        instrumentation.addAttributesExtractor(constant(stringKey("test.key"), "abc"));
        instrumentation.install(mock(), openTelemetryRum);

        String exceptionMessage = "boooom!";
        RuntimeException crash = new RuntimeException(exceptionMessage);
        Thread crashingThread =
                new Thread(
                        () -> {
                            throw crash;
                        });
        crashingThread.setDaemon(true);
        crashingThread.start();
        crashingThread.join();

        List<LogRecordData> logRecords = testing.getLogRecords();
        assertThat(logRecords).hasSize(1);

        Attributes crashAttributes = logRecords.get(0).getAttributes();
        OpenTelemetryAssertions.assertThat(crashAttributes)
                .containsEntry(ExceptionAttributes.EXCEPTION_ESCAPED, true)
                .containsEntry(ExceptionAttributes.EXCEPTION_MESSAGE, exceptionMessage)
                .containsEntry(ExceptionAttributes.EXCEPTION_TYPE, "java.lang.RuntimeException")
                .containsEntry(ThreadIncubatingAttributes.THREAD_ID, crashingThread.getId())
                .containsEntry(ThreadIncubatingAttributes.THREAD_NAME, crashingThread.getName())
                .containsEntry(stringKey("test.key"), "abc");
        assertThat(crashAttributes.get(ExceptionAttributes.EXCEPTION_STACKTRACE))
                .startsWith("java.lang.RuntimeException: boooom!");
    }
}
