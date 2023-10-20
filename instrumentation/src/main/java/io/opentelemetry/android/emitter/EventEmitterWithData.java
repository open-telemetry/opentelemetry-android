/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.emitter;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.events.EventEmitter;
import java.util.Map;

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

    public Attributes convertRichDataObjectIntoAttributes(Map<String, Object> data) {
        AttributesBuilder builder = Attributes.builder();
        for (String key : data.keySet()) {
            Object val = data.get(key);
            if (val != null) {
                if (val instanceof String) {
                    builder.put(AttributeKey.stringKey(key), (String) val);
                } else if (val instanceof Long) {
                    builder.put(AttributeKey.longKey(key), (Long) val);
                } else if (val instanceof Double) {
                    builder.put(AttributeKey.doubleKey(key), (Double) val);
                } else if (val instanceof Boolean) {
                    builder.put(AttributeKey.booleanKey(key), (Boolean) val);
                } else {
                    builder.put(AttributeKey.stringKey(key), val.toString());
                }
            }
        }
        return builder.build();
    }
}
