/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.anr;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Looper;
import io.opentelemetry.android.instrumentation.common.InstrumentedApplication;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnrDetectorTest {

    @Mock Looper mainLooper;
    @Mock ScheduledExecutorService scheduler;
    @Mock InstrumentedApplication instrumentedApplication;

    @Test
    void shouldInstallInstrumentation() {
        when(instrumentedApplication.getOpenTelemetrySdk())
                .thenReturn(OpenTelemetrySdk.builder().build());

        AnrDetector anrDetector =
                AnrDetector.builder()
                        .setMainLooper(mainLooper)
                        .setScheduler(scheduler)
                        .addAttributesExtractor(constant(stringKey("test.key"), "abc"))
                        .build();
        anrDetector.installOn(instrumentedApplication);

        // verify that the ANR scheduler was started
        verify(scheduler)
                .scheduleWithFixedDelay(
                        isA(AnrWatcher.class), eq(1L), eq(1L), eq(TimeUnit.SECONDS));
        // verify that an application listener was installed
        verify(instrumentedApplication)
                .registerApplicationStateListener(isA(AnrDetectorToggler.class));
    }
}
