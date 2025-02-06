/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.android.session.SessionManager;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.semconv.ExceptionAttributes;
import io.opentelemetry.semconv.incubating.ThreadIncubatingAttributes;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

@RunWith(AndroidJUnit4.class)
public class CrashReporterTest {
    private OpenTelemetrySdk openTelemetrySdk;
    private InMemoryLogRecordExporter logRecordExporter;
    private Thread.UncaughtExceptionHandler existingHandler;

    @Before
    public void setUp() {
        logRecordExporter = InMemoryLogRecordExporter.create();
        openTelemetrySdk =
                OpenTelemetrySdk.builder()
                        .setLoggerProvider(
                                SdkLoggerProvider.builder()
                                        .addLogRecordProcessor(
                                                SimpleLogRecordProcessor.create(logRecordExporter))
                                        .build())
                        .build();
        existingHandler = Thread.getDefaultUncaughtExceptionHandler();
        // disable the handler installed by junit
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    @After
    public void tearDown() {
        Thread.setDefaultUncaughtExceptionHandler(existingHandler);
    }

    @Test
    public void integrationTest() throws InterruptedException {
        CrashReporterInstrumentation instrumentation = new CrashReporterInstrumentation();
        instrumentation.addAttributesExtractor(constant(stringKey("test.key"), "abc"));
        InstallationContext ctx =
                new InstallationContext(
                        RuntimeEnvironment.getApplication(),
                        openTelemetrySdk,
                        mock(SessionManager.class));
        instrumentation.install(ctx);

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

        List<LogRecordData> logRecords = logRecordExporter.getFinishedLogRecordItems();
        assertThat(logRecords).hasSize(1);

        Attributes crashAttributes = logRecords.get(0).getAttributes();
        OpenTelemetryAssertions.assertThat(crashAttributes)
                .containsEntry(ExceptionAttributes.EXCEPTION_MESSAGE, exceptionMessage)
                .containsEntry(ExceptionAttributes.EXCEPTION_TYPE, "java.lang.RuntimeException")
                .containsEntry(ThreadIncubatingAttributes.THREAD_ID, crashingThread.getId())
                .containsEntry(ThreadIncubatingAttributes.THREAD_NAME, crashingThread.getName())
                .containsEntry(stringKey("test.key"), "abc");
        assertThat(crashAttributes.get(ExceptionAttributes.EXCEPTION_STACKTRACE))
                .startsWith("java.lang.RuntimeException: boooom!");
    }
}
