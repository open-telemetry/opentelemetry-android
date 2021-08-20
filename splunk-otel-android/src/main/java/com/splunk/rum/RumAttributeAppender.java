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

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_TYPE;

import android.os.Build;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

class RumAttributeAppender implements SpanProcessor {
    static final AttributeKey<String> APP_NAME_KEY = stringKey("app");
    static final AttributeKey<String> SESSION_ID_KEY = stringKey("splunk.rumSessionId");
    static final AttributeKey<String> DEVICE_MODEL_NAME_KEY = ResourceAttributes.DEVICE_MODEL_NAME;
    static final AttributeKey<String> DEVICE_MODEL_KEY = ResourceAttributes.DEVICE_MODEL_IDENTIFIER;
    static final AttributeKey<String> RUM_VERSION_KEY = stringKey("splunk.rumVersion");
    static final AttributeKey<String> OS_VERSION_KEY = ResourceAttributes.OS_VERSION;

    //note: these will be in the otel semantic conventions as of spec v1.6.0
    static final AttributeKey<String> NETWORK_TYPE_KEY = stringKey("host.connection.type");
    static final AttributeKey<String> NETWORK_SUBTYPE_KEY = stringKey("host.connection.subtype");
    static final AttributeKey<String> SPLUNK_OPERATION_KEY = stringKey("_splunk_operation");

    private final Config config;
    private final SessionId sessionId;
    private final String rumVersion;
    private final VisibleScreenTracker visibleScreenTracker;
    private final ConnectionUtil connectionUtil;

    RumAttributeAppender(Config config, SessionId sessionId, String rumVersion, VisibleScreenTracker visibleScreenTracker, ConnectionUtil connectionUtil) {
        this.config = config;
        this.sessionId = sessionId;
        this.rumVersion = rumVersion;
        this.visibleScreenTracker = visibleScreenTracker;
        this.connectionUtil = connectionUtil;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        //set this custom attribute in order to let the CustomZipkinEncoder use it for the span name on the wire.
        span.setAttribute(SPLUNK_OPERATION_KEY, span.getName());
        span.setAttribute(APP_NAME_KEY, config.getApplicationName());
        span.setAttribute(SESSION_ID_KEY, sessionId.getSessionId());

        span.setAttribute(DEVICE_MODEL_NAME_KEY, Build.MODEL);
        span.setAttribute(DEVICE_MODEL_KEY, Build.MODEL);
        span.setAttribute(OS_VERSION_KEY, Build.VERSION.RELEASE);
        span.setAttribute(RUM_VERSION_KEY, rumVersion);
        span.setAttribute(OS_TYPE, "Android");
        span.setAllAttributes(config.getGlobalAttributes());

        String currentScreen = visibleScreenTracker.getCurrentlyVisibleScreen();
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, currentScreen);
        CurrentNetwork currentNetwork = connectionUtil.getActiveNetwork();
        span.setAttribute(NETWORK_TYPE_KEY, currentNetwork.getState().getHumanName());
        currentNetwork.getSubType().ifPresent(subtype -> span.setAttribute(NETWORK_SUBTYPE_KEY, subtype));
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
