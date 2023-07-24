/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrashReportingExceptionHandlerTest {

    @Mock Instrumenter<CrashDetails, Void> instrumenter;
    @Mock SdkTracerProvider sdkTracerProvider;
    @Mock Thread.UncaughtExceptionHandler existingHandler;
    @Mock CompletableResultCode flushResult;

    @Test
    void shouldReportCrash() {
        when(sdkTracerProvider.forceFlush()).thenReturn(flushResult);

        CrashReportingExceptionHandler handler =
                new CrashReportingExceptionHandler(
                        instrumenter, sdkTracerProvider, existingHandler);

        NullPointerException oopsie = new NullPointerException("oopsie");
        Thread crashThread = new Thread("badThread");

        handler.uncaughtException(crashThread, oopsie);

        CrashDetails crashDetails = CrashDetails.create(crashThread, oopsie);
        InOrder io = inOrder(instrumenter, sdkTracerProvider, flushResult, existingHandler);
        io.verify(instrumenter).start(Context.current(), crashDetails);
        io.verify(instrumenter).end(any(), eq(crashDetails), isNull(), eq(oopsie));
        io.verify(sdkTracerProvider).forceFlush();
        io.verify(flushResult).join(10, TimeUnit.SECONDS);
        io.verify(existingHandler).uncaughtException(crashThread, oopsie);
        io.verifyNoMoreInteractions();
    }
}
