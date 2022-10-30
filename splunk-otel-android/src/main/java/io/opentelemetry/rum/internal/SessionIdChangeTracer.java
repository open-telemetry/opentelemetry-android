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
import io.opentelemetry.api.trace.Tracer;

final class SessionIdChangeTracer implements SessionIdChangeListener {

    static final AttributeKey<String> PREVIOUS_SESSION_ID_KEY =
            stringKey("splunk.rum.previous_session_id");

    private final Tracer tracer;

    SessionIdChangeTracer(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void onChange(String oldSessionId, String newSessionId) {
        tracer.spanBuilder("sessionId.change")
                .setAttribute(PREVIOUS_SESSION_ID_KEY, oldSessionId)
                .startSpan()
                .end();
    }
}
