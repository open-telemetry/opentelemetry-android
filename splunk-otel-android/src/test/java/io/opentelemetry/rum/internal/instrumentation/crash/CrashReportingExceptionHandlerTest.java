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
