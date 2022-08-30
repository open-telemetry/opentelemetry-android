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
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEVICE_MODEL_IDENTIFIER;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.DEVICE_MODEL_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_NAME;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_TYPE;
import static io.opentelemetry.semconv.resource.attributes.ResourceAttributes.OS_VERSION;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_TYPE;

import android.os.Build;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import java.util.function.Supplier;

class RumAttributeAppender implements SpanProcessor {
    static final AttributeKey<String> APP_NAME_KEY = stringKey("app");
    static final AttributeKey<String> SESSION_ID_KEY = stringKey("splunk.rumSessionId");
    static final AttributeKey<String> RUM_VERSION_KEY = stringKey("splunk.rum.version");

    static final AttributeKey<String> SPLUNK_OPERATION_KEY = stringKey("_splunk_operation");

    private final String applicationName;
    private final Supplier<Attributes> globalAttributesSupplier;
    private final SessionId sessionId;
    private final String rumVersion;
    private final VisibleScreenTracker visibleScreenTracker;
    private final ConnectionUtil connectionUtil;

    RumAttributeAppender(
            String applicationName,
            Supplier<Attributes> globalAttributesSupplier,
            SessionId sessionId,
            String rumVersion,
            VisibleScreenTracker visibleScreenTracker,
            ConnectionUtil connectionUtil) {
        this.applicationName = applicationName;
        this.globalAttributesSupplier = globalAttributesSupplier;
        this.sessionId = sessionId;
        this.rumVersion = rumVersion;
        this.visibleScreenTracker = visibleScreenTracker;
        this.connectionUtil = connectionUtil;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // set this custom attribute in order to let the CustomZipkinEncoder use it for the span
        // name on the wire.
        span.setAttribute(SPLUNK_OPERATION_KEY, span.getName());

        span.setAttribute(APP_NAME_KEY, applicationName);
        span.setAttribute(SESSION_ID_KEY, sessionId.getSessionId());
        span.setAttribute(RUM_VERSION_KEY, rumVersion);

        span.setAttribute(DEVICE_MODEL_NAME, Build.MODEL);
        span.setAttribute(DEVICE_MODEL_IDENTIFIER, Build.MODEL);
        span.setAttribute(OS_NAME, "Android");
        span.setAttribute(OS_TYPE, "linux");
        span.setAttribute(OS_VERSION, Build.VERSION.RELEASE);
        span.setAllAttributes(globalAttributesSupplier.get());

        String currentScreen = visibleScreenTracker.getCurrentlyVisibleScreen();
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, currentScreen);
        CurrentNetwork currentNetwork = connectionUtil.getActiveNetwork();
        span.setAttribute(NET_HOST_CONNECTION_TYPE, currentNetwork.getState().getHumanName());
        currentNetwork
                .getSubType()
                .ifPresent(subtype -> span.setAttribute(NET_HOST_CONNECTION_SUBTYPE, subtype));
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
