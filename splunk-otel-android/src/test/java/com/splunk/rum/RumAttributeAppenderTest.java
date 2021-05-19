package com.splunk.rum;

import org.junit.Test;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RumAttributeAppenderTest {

    @Test
    public void interfaceMethods() {

        RumAttributeAppender rumAttributeAppender = new RumAttributeAppender(mock(Config.class), mock(SessionId.class));

        assertTrue(rumAttributeAppender.isStartRequired());
        assertFalse(rumAttributeAppender.isEndRequired());
    }

    @Test
    public void appendAttributesOnStart() {
        Config config = mock(Config.class);
        when(config.getApplicationName()).thenReturn("appName");
        SessionId sessionId = mock(SessionId.class);
        when(sessionId.getSessionId()).thenReturn("rumSessionId");

        ReadWriteSpan span = mock(ReadWriteSpan.class);

        RumAttributeAppender rumAttributeAppender = new RumAttributeAppender(config, sessionId);

        rumAttributeAppender.onStart(Context.current(), span);
        verify(span).setAttribute(RumAttributeAppender.APP_NAME_KEY, "appName");
        verify(span).setAttribute(RumAttributeAppender.SESSION_ID_KEY, "rumSessionId");
    }
}