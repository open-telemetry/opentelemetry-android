package com.splunk.rum;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

class RumAttributeAppender implements SpanProcessor {
    static final AttributeKey<String> APP_NAME_KEY = stringKey("app");
    static final AttributeKey<String> SESSION_ID_KEY = stringKey("splunk.rumSessionId");

    private final Config config;
    private final SessionId sessionId;

    RumAttributeAppender(Config config, SessionId sessionId) {
        this.config = config;
        this.sessionId = sessionId;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        span.setAttribute(APP_NAME_KEY, config.getApplicationName());
        span.setAttribute(SESSION_ID_KEY, sessionId.getSessionId());
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
