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

import static com.splunk.rum.SplunkRum.ERROR_MESSAGE_KEY;
import static com.splunk.rum.SplunkRum.ERROR_TYPE_KEY;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.EXCEPTION_TYPE;

import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.DelegatingSpanData;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

final class ModifiedSpanData extends DelegatingSpanData {

    private final List<EventData> modifiedEvents;
    private final Attributes modifiedAttributes;

    static SpanData create(SpanData original) {
        return create(original, original.getAttributes().toBuilder());
    }

    static SpanData create(SpanData original, AttributesBuilder modifiedAttributes) {
        // zipkin eats the event attributes that are recorded by default, so we need to convert
        // the exception event to span attributes
        List<EventData> modifiedEvents = new ArrayList<>(original.getEvents().size());
        for (EventData event : original.getEvents()) {
            if (event.getName().equals(SemanticAttributes.EXCEPTION_EVENT_NAME)) {
                modifiedAttributes.putAll(extractExceptionAttributes(event));
            } else {
                // if it's not an exception, leave the event as it is
                modifiedEvents.add(event);
            }
        }

        return new ModifiedSpanData(original, modifiedEvents, modifiedAttributes.build());
    }

    private static Attributes extractExceptionAttributes(EventData event) {
        String type = event.getAttributes().get(EXCEPTION_TYPE);
        String message = event.getAttributes().get(EXCEPTION_MESSAGE);
        String stacktrace = event.getAttributes().get(EXCEPTION_STACKTRACE);

        AttributesBuilder builder = Attributes.builder();
        if (type != null) {
            int dot = type.lastIndexOf('.');
            String simpleType = dot == -1 ? type : type.substring(dot + 1);
            builder.put(EXCEPTION_TYPE, simpleType);
            // this attribute's here to support the RUM UI/backend until it can be updated to use otel conventions.
            builder.put(ERROR_TYPE_KEY, simpleType);
        }
        if (message != null) {
            builder.put(EXCEPTION_MESSAGE, message);
            // this attribute's here to support the RUM UI/backend until it can be updated to use otel conventions.
            builder.put(ERROR_MESSAGE_KEY, message);
        }
        if (stacktrace != null) {
            builder.put(EXCEPTION_STACKTRACE, stacktrace);
        }
        return builder.build();
    }

    ModifiedSpanData(SpanData original, List<EventData> modifiedEvents, Attributes modifiedAttributes) {
        super(original);
        this.modifiedEvents = modifiedEvents;
        this.modifiedAttributes = modifiedAttributes;
    }

    @Override
    public List<EventData> getEvents() {
        return modifiedEvents;
    }

    @Override
    public int getTotalRecordedEvents() {
        return modifiedEvents.size();
    }

    @Override
    public Attributes getAttributes() {
        return modifiedAttributes;
    }

    @Override
    public int getTotalAttributeCount() {
        return modifiedAttributes.size();
    }
}
