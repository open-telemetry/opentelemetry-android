/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android;

import static io.opentelemetry.android.common.RumConstants.SCREEN_NAME_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.android.internal.services.visiblescreen.VisibleScreenTracker;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.junit.jupiter.api.Test;

class ScreenAttributesSpanProcessorTest {

    @Test
    void append() {
        String screenName = "my cool screen";
        VisibleScreenTracker visibleScreenTracker = mock(VisibleScreenTracker.class);
        Context contenxt = mock(Context.class);
        ReadWriteSpan span = mock(ReadWriteSpan.class);

        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn(screenName);

        ScreenAttributesSpanProcessor testClass =
                new ScreenAttributesSpanProcessor(visibleScreenTracker);
        assertThat(testClass.isStartRequired()).isTrue();
        assertThat(testClass.isEndRequired()).isFalse();
        assertThatCode(() -> testClass.onEnd(mock(ReadableSpan.class))).doesNotThrowAnyException();

        testClass.onStart(contenxt, span);
        verify(span).setAttribute(SCREEN_NAME_KEY, screenName);
    }
}
