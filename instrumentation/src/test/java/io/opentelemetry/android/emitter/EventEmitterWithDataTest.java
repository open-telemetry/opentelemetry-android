/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.emitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class EventEmitterWithDataTest {

    @Test
    public void testConvertRichDataObjectIntoAttributes() {
        Exception crashData = null;
        try {
            throw new RuntimeException("Crash!");
        } catch (Exception e) {
            crashData = e;
        }

        StringWriter stacktrace = new StringWriter();
        crashData.printStackTrace(new PrintWriter(stacktrace));

        Map<String, Object> data = new HashMap<>();
        data.put("stringKey", "key");
        data.put("longKey", 123L);
        data.put("doubleKey", 12.3);
        data.put("booleanKey", true);

        data.put("stacktrace", stacktrace.toString());
        data.put("thread", Thread.currentThread().toString());
        data.put("throwableMessage", crashData.getMessage());

        EventEmitterWithData emitter = new EventEmitterWithData(null);
        Attributes attributes = emitter.convertRichDataObjectIntoAttributes(data);

        assertEquals(attributes.size(), data.size());
        assertEquals(data.get("stacktrace"), stacktrace.toString());
        assertEquals(data.get("thread"), Thread.currentThread().toString());
        assertEquals(data.get("throwableMessage"), "Crash!");
        assertEquals(data.get("stringKey"), attributes.get(AttributeKey.stringKey("stringKey")));
        assertEquals(data.get("longKey"), attributes.get(AttributeKey.longKey("longKey")));
        assertEquals(data.get("doubleKey"), attributes.get(AttributeKey.doubleKey("doubleKey")));
        assertEquals(data.get("booleanKey"), attributes.get(AttributeKey.booleanKey("booleanKey")));
    }
}
