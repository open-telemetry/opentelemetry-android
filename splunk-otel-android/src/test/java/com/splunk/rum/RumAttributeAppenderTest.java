package com.splunk.rum;

import org.junit.Before;
import org.junit.Test;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RumAttributeAppenderTest {

    private VisibleScreenTracker visibleScreenTracker;

    @Before
    public void setUp() {
        visibleScreenTracker = mock(VisibleScreenTracker.class);
    }

    @Test
    public void interfaceMethods() {
        RumAttributeAppender rumAttributeAppender = new RumAttributeAppender(mock(Config.class), mock(SessionId.class), "rumVersion", visibleScreenTracker);

        assertTrue(rumAttributeAppender.isStartRequired());
        assertFalse(rumAttributeAppender.isEndRequired());
    }

    @Test
    public void appendAttributesOnStart() {
        Config config = mock(Config.class);
        when(config.getApplicationName()).thenReturn("appName");
        SessionId sessionId = mock(SessionId.class);
        when(sessionId.getSessionId()).thenReturn("rumSessionId");
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("ScreenOne");

        ReadWriteSpan span = mock(ReadWriteSpan.class);

        RumAttributeAppender rumAttributeAppender = new RumAttributeAppender(config, sessionId, "rumVersion", visibleScreenTracker);

        rumAttributeAppender.onStart(Context.current(), span);
        verify(span).setAttribute(RumAttributeAppender.RUM_VERSION_KEY, "rumVersion");
        verify(span).setAttribute(RumAttributeAppender.APP_NAME_KEY, "appName");
        verify(span).setAttribute(RumAttributeAppender.SESSION_ID_KEY, "rumSessionId");
        verify(span).setAttribute(ResourceAttributes.OS_TYPE, "Android");
        verify(span).setAttribute(SplunkRum.SCREEN_NAME_KEY, "ScreenOne");

        //these values don't seem to be available in unit tests, so just assert that something was set.
        verify(span).setAttribute(eq(RumAttributeAppender.DEVICE_MODEL_KEY), any());
        verify(span).setAttribute(eq(RumAttributeAppender.DEVICE_MODEL_NAME_KEY), any());
        verify(span).setAttribute(eq(RumAttributeAppender.OS_VERSION_KEY), any());
    }

    @Test
    public void appendAttributes_noCurrentScreens() {
        Config config = mock(Config.class);
        when(config.getApplicationName()).thenReturn("appName");
        SessionId sessionId = mock(SessionId.class);
        when(sessionId.getSessionId()).thenReturn("rumSessionId");
        when(visibleScreenTracker.getCurrentlyVisibleScreen()).thenReturn("unknown");
        when(visibleScreenTracker.getPreviouslyVisibleScreen()).thenReturn(null);

        ReadWriteSpan span = mock(ReadWriteSpan.class);

        RumAttributeAppender rumAttributeAppender = new RumAttributeAppender(config, sessionId, "rumVersion", visibleScreenTracker);

        rumAttributeAppender.onStart(Context.current(), span);
        verify(span).setAttribute(SplunkRum.SCREEN_NAME_KEY, "unknown");
        verify(span, never()).setAttribute(eq(SplunkRum.LAST_SCREEN_NAME_KEY), any());
    }
}