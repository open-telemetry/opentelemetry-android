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
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_ICC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_MCC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_MNC;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CARRIER_NAME;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_SUBTYPE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NET_HOST_CONNECTION_TYPE;

import androidx.annotation.Nullable;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

class RumAttributeAppender implements SpanProcessor {

    static final AttributeKey<String> SESSION_ID_KEY = stringKey("splunk.rumSessionId");

    private final VisibleScreenTracker visibleScreenTracker;
    private final ConnectionUtil connectionUtil;

    RumAttributeAppender(VisibleScreenTracker visibleScreenTracker, ConnectionUtil connectionUtil) {
        this.visibleScreenTracker = visibleScreenTracker;
        this.connectionUtil = connectionUtil;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        String currentScreen = visibleScreenTracker.getCurrentlyVisibleScreen();
        span.setAttribute(SplunkRum.SCREEN_NAME_KEY, currentScreen);

        CurrentNetwork currentNetwork = connectionUtil.getActiveNetwork();
        appendNetworkAttributes(span, currentNetwork);
    }

    static void appendNetworkAttributes(Span span, CurrentNetwork currentNetwork) {
        setIfNotNull(span, NET_HOST_CONNECTION_TYPE, currentNetwork.getState().getHumanName());
        setIfNotNull(span, NET_HOST_CONNECTION_SUBTYPE, currentNetwork.getSubType());
        setIfNotNull(span, NET_HOST_CARRIER_NAME, currentNetwork.getCarrierName());
        setIfNotNull(span, NET_HOST_CARRIER_MCC, currentNetwork.getCarrierCountryCode());
        setIfNotNull(span, NET_HOST_CARRIER_MNC, currentNetwork.getCarrierNetworkCode());
        setIfNotNull(span, NET_HOST_CARRIER_ICC, currentNetwork.getCarrierIsoCountryCode());
    }

    private static void setIfNotNull(Span span, AttributeKey<String> key, @Nullable String value) {
        if (value != null) {
            span.setAttribute(key, value);
        }
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
