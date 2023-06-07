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

import static io.opentelemetry.rum.internal.RumConstants.LAST_SCREEN_NAME_KEY;
import static io.opentelemetry.rum.internal.RumConstants.SCREEN_NAME_KEY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.context.Context;
import io.opentelemetry.rum.internal.instrumentation.activity.VisibleScreenTracker;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScreenAttributesAppenderTest {

    private VisibleScreenTracker visibleScreenTracker;

    @BeforeEach
    void setUp() {
        visibleScreenTracker = mock(VisibleScreenTracker.class);
    }

    @Test
    void interfaceMethods() {
        ScreenAttributesAppender screenAttributesAppender =
                new ScreenAttributesAppender(visibleScreenTracker);

        assertTrue(screenAttributesAppender.isStartRequired());
        assertFalse(screenAttributesAppender.isEndRequired());
    }

    @Test
    void appendAttributesOnStart() {
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("ScreenOne");

        ReadWriteSpan span = mock(ReadWriteSpan.class);

        ScreenAttributesAppender screenAttributesAppender =
                new ScreenAttributesAppender(visibleScreenTracker);

        screenAttributesAppender.onStart(Context.current(), span);
        verify(span).setAttribute(SCREEN_NAME_KEY, "ScreenOne");
    }

    @Test
    void appendAttributes_noCurrentScreens() {
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("unknown");

        ReadWriteSpan span = mock(ReadWriteSpan.class);

        ScreenAttributesAppender screenAttributesAppender =
                new ScreenAttributesAppender(visibleScreenTracker);

        screenAttributesAppender.onStart(Context.current(), span);
        verify(span).setAttribute(SCREEN_NAME_KEY, "unknown");
        verify(span, never()).setAttribute(eq(LAST_SCREEN_NAME_KEY), any());
    }
}
