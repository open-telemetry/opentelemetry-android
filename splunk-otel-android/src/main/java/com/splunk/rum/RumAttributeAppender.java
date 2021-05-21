package com.splunk.rum;

import android.os.Build;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_TYPE;

class RumAttributeAppender implements SpanProcessor {
    static final AttributeKey<String> APP_NAME_KEY = stringKey("app");
    static final AttributeKey<String> SESSION_ID_KEY = stringKey("splunk.rumSessionId");
    static final AttributeKey<String> DEVICE_MODEL_NAME_KEY = stringKey("device.model.name");
    static final AttributeKey<String> DEVICE_MODEL_KEY = stringKey("device.model");
    static final AttributeKey<String> RUM_VERSION_KEY = stringKey("splunk.rumVersion");
    static final AttributeKey<String> OS_VERSION_KEY = stringKey("os.version");

    private final Config config;
    private final SessionId sessionId;
    private final String rumVersion;

    RumAttributeAppender(Config config, SessionId sessionId, String rumVersion) {
        this.config = config;
        this.sessionId = sessionId;
        this.rumVersion = rumVersion;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        span.setAttribute(APP_NAME_KEY, config.getApplicationName());
        span.setAttribute(SESSION_ID_KEY, sessionId.getSessionId());

        span.setAttribute(DEVICE_MODEL_NAME_KEY, Build.MODEL);
        span.setAttribute(DEVICE_MODEL_KEY, Build.MODEL);
        span.setAttribute(OS_VERSION_KEY, Build.VERSION.RELEASE);
        span.setAttribute(RUM_VERSION_KEY, rumVersion);
        span.setAttribute(OS_TYPE, "Android");
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
