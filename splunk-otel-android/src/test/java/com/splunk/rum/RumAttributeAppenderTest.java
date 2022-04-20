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

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.junit.Before;
import org.junit.Test;

public class RumAttributeAppenderTest {

    private VisibleScreenTracker visibleScreenTracker;
    private final ConnectionUtil connectionUtil = mock(ConnectionUtil.class);

    @Before
    public void setUp() {
        visibleScreenTracker = mock(VisibleScreenTracker.class);
        when(connectionUtil.getActiveNetwork())
                .thenReturn(new CurrentNetwork(NetworkState.TRANSPORT_CELLULAR, "LTE"));
    }

    @Test
    public void interfaceMethods() {
        Config config = mock(Config.class);
        when(config.getGlobalAttributes()).thenReturn(Attributes.empty());
        RumAttributeAppender rumAttributeAppender =
                new RumAttributeAppender(
                        config,
                        mock(SessionId.class),
                        "rumVersion",
                        visibleScreenTracker,
                        connectionUtil);

        assertTrue(rumAttributeAppender.isStartRequired());
        assertFalse(rumAttributeAppender.isEndRequired());
    }

    @Test
    public void updateGlobalAttributes() {
        Attributes initialAttributes =
                Attributes.of(stringKey("cheese"), "Camembert", longKey("size"), 5L);

        Config config =
                Config.builder()
                        .globalAttributes(initialAttributes)
                        .realm("us0")
                        .rumAccessToken("123456")
                        .applicationName("appName")
                        .build();

        RumAttributeAppender rumAttributeAppender =
                new RumAttributeAppender(
                        config,
                        new SessionId(new SessionIdTimeoutHandler()),
                        "version",
                        visibleScreenTracker,
                        connectionUtil);

        ReadWriteSpan span = mock(ReadWriteSpan.class);
        rumAttributeAppender.onStart(Context.current(), span);
        verify(span).setAllAttributes(initialAttributes);

        config.updateGlobalAttributes(
                attributesBuilder -> attributesBuilder.put("cheese", "cheddar"));

        span = mock(ReadWriteSpan.class);
        rumAttributeAppender.onStart(Context.current(), span);

        Attributes updatedAttributes =
                Attributes.of(stringKey("cheese"), "cheddar", longKey("size"), 5L);
        verify(span).setAllAttributes(updatedAttributes);
    }

    @Test
    public void appendAttributesOnStart() {
        Attributes globalAttributes =
                Attributes.of(stringKey("cheese"), "Camembert", longKey("size"), 5L);
        Config config = mock(Config.class);
        when(config.getApplicationName()).thenReturn("appName");
        when(config.getGlobalAttributes()).thenReturn(globalAttributes);

        SessionId sessionId = mock(SessionId.class);
        when(sessionId.getSessionId()).thenReturn("rumSessionId");
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("ScreenOne");

        ReadWriteSpan span = mock(ReadWriteSpan.class);

        RumAttributeAppender rumAttributeAppender =
                new RumAttributeAppender(
                        config, sessionId, "rumVersion", visibleScreenTracker, connectionUtil);

        rumAttributeAppender.onStart(Context.current(), span);
        verify(span).setAttribute(RumAttributeAppender.RUM_VERSION_KEY, "rumVersion");
        verify(span).setAttribute(RumAttributeAppender.APP_NAME_KEY, "appName");
        verify(span).setAttribute(RumAttributeAppender.SESSION_ID_KEY, "rumSessionId");
        verify(span).setAttribute(ResourceAttributes.OS_TYPE, "linux");
        verify(span).setAttribute(ResourceAttributes.OS_NAME, "Android");
        verify(span).setAttribute(SplunkRum.SCREEN_NAME_KEY, "ScreenOne");
        verify(span).setAttribute(SemanticAttributes.NET_HOST_CONNECTION_TYPE, "cell");
        verify(span).setAttribute(SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE, "LTE");

        // these values don't seem to be available in unit tests, so just assert that something was
        // set.
        verify(span).setAttribute(eq(ResourceAttributes.DEVICE_MODEL_IDENTIFIER), any());
        verify(span).setAttribute(eq(ResourceAttributes.DEVICE_MODEL_NAME), any());
        verify(span).setAttribute(eq(ResourceAttributes.OS_VERSION), any());
        verify(span).setAllAttributes(globalAttributes);
    }

    @Test
    public void appendAttributes_noCurrentScreens() {
        Config config = mock(Config.class);
        when(config.getApplicationName()).thenReturn("appName");
        when(config.getGlobalAttributes()).thenReturn(Attributes.empty());
        SessionId sessionId = mock(SessionId.class);
        when(sessionId.getSessionId()).thenReturn("rumSessionId");
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("unknown");

        ReadWriteSpan span = mock(ReadWriteSpan.class);

        RumAttributeAppender rumAttributeAppender =
                new RumAttributeAppender(
                        config, sessionId, "rumVersion", visibleScreenTracker, connectionUtil);

        rumAttributeAppender.onStart(Context.current(), span);
        verify(span).setAttribute(SplunkRum.SCREEN_NAME_KEY, "unknown");
        verify(span, never()).setAttribute(eq(SplunkRum.LAST_SCREEN_NAME_KEY), any());
    }
}
