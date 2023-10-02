/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.emitter;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.events.EventEmitter;
import java.util.Map;
import java.util.Objects;

class EventEmitterWithData {

    private static final String EVENT_DATA = "event.data";

    private final EventEmitter delegate;

    EventEmitterWithData(EventEmitter delegate) {
        this.delegate = delegate;
    }

    public void emit(String eventName, Attributes attributes, Map<String, Object> data) {
        Attributes dataAsAttribute = convertRichDataObjectIntoAttributes(data);
        Attributes eventAttrs =
                attributes.toBuilder().put(EVENT_DATA, dataAsAttribute.toString()).build();
        delegate.emit(eventName, eventAttrs);
    }

    private Attributes convertRichDataObjectIntoAttributes(Map<String, Object> data) {
        AttributesBuilder builder = Attributes.builder();
        for (String key : data.keySet()) {
            Object val = data.get(key);
            if (val != null) {
                if (val instanceof String) {
                    builder.put(key, (String) Objects.requireNonNull(data.get(key)));
                }
                if (val instanceof Long) {
                    builder.put(key, (Long) Objects.requireNonNull(data.get(key)));
                }
                if (val instanceof Double) {
                    builder.put(key, (Double) Objects.requireNonNull(data.get(key)));
                }
                if (val instanceof Boolean) {
                    builder.put(key, (Boolean) Objects.requireNonNull(data.get(key)));
                } else {
                    builder.put(key, (String) Objects.requireNonNull(data.get(key)).toString());
                }
            }
        }
        return builder.build();
    }
}
