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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RumAttributeAppenderTest {

    private VisibleScreenTracker visibleScreenTracker;
    private final ConnectionUtil connectionUtil = mock(ConnectionUtil.class);

    @BeforeEach
    void setUp() {
        visibleScreenTracker = mock(VisibleScreenTracker.class);
        when(connectionUtil.getActiveNetwork())
                .thenReturn(
                        CurrentNetwork.builder(NetworkState.TRANSPORT_CELLULAR)
                                .subType("LTE")
                                .build());
    }

    @Test
    void interfaceMethods() {
        RumAttributeAppender rumAttributeAppender =
                new RumAttributeAppender(visibleScreenTracker, connectionUtil);

        assertTrue(rumAttributeAppender.isStartRequired());
        assertFalse(rumAttributeAppender.isEndRequired());
    }

    @Test
    void appendAttributesOnStart() {
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("ScreenOne");

        ReadWriteSpan span = mock(ReadWriteSpan.class);

        RumAttributeAppender rumAttributeAppender =
                new RumAttributeAppender(visibleScreenTracker, connectionUtil);

        rumAttributeAppender.onStart(Context.current(), span);
        verify(span).setAttribute(SplunkRum.SCREEN_NAME_KEY, "ScreenOne");
        verify(span).setAttribute(SemanticAttributes.NET_HOST_CONNECTION_TYPE, "cell");
        verify(span).setAttribute(SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE, "LTE");
    }

    @Test
    void appendAttributes_noCurrentScreens() {
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("unknown");

        ReadWriteSpan span = mock(ReadWriteSpan.class);

        RumAttributeAppender rumAttributeAppender =
                new RumAttributeAppender(visibleScreenTracker, connectionUtil);

        rumAttributeAppender.onStart(Context.current(), span);
        verify(span).setAttribute(SplunkRum.SCREEN_NAME_KEY, "unknown");
        verify(span, never()).setAttribute(eq(SplunkRum.LAST_SCREEN_NAME_KEY), any());
    }
}
