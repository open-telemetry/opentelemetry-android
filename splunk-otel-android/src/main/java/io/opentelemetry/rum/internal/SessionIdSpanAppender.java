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

package io.opentelemetry.rum.internal;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

final class SessionIdSpanAppender implements SpanProcessor {

    // TODO: rename to something that is not splunk specific
    static final AttributeKey<String> SESSION_ID_KEY = stringKey("splunk.rumSessionId");

    private final SessionId sessionId;

    public SessionIdSpanAppender(SessionId sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        span.setAttribute(SESSION_ID_KEY, sessionId.getSessionId());
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
