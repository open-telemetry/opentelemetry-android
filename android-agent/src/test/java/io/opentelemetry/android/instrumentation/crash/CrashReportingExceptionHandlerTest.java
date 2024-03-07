/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrashReportingExceptionHandlerTest {

    @Mock Consumer<CrashDetails> crashSender;
    @Mock SdkLoggerProvider sdkLoggerProvider;
    @Mock Thread.UncaughtExceptionHandler existingHandler;
    @Mock CompletableResultCode flushResult;

    @Test
    void shouldReportCrash() {
        when(sdkLoggerProvider.forceFlush()).thenReturn(flushResult);

        CrashReportingExceptionHandler handler =
                new CrashReportingExceptionHandler(crashSender, sdkLoggerProvider, existingHandler);

        NullPointerException oopsie = new NullPointerException("oopsie");
        Thread crashThread = new Thread("badThread");

        handler.uncaughtException(crashThread, oopsie);

        CrashDetails crashDetails = CrashDetails.create(crashThread, oopsie);
        InOrder io = inOrder(crashSender, sdkLoggerProvider, flushResult, existingHandler);
        io.verify(crashSender).accept(crashDetails);
        io.verify(sdkLoggerProvider).forceFlush();
        io.verify(flushResult).join(10, TimeUnit.SECONDS);
        io.verify(existingHandler).uncaughtException(crashThread, oopsie);
        io.verifyNoMoreInteractions();
    }
}
