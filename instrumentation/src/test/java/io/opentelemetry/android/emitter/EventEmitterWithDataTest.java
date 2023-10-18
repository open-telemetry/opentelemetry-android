package io.opentelemetry.android.emitter;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventEmitterWithDataTest {

    @Test
    public void testConvertRichDataObjectIntoAttributes() {
        Map<String, Object> data = new HashMap<>();
        data.put("stringKey", "key");
        data.put("longKey", 123L);
        data.put("doubleKey", 12.3);
        data.put("booleanKey", true);

        EventEmitterWithData emitter = new EventEmitterWithData(null);

        Attributes attributes = emitter.convertRichDataObjectIntoAttributes(data);

        assertEquals(attributes.size(), data.size());
        assertEquals(data.get("stringKey"), attributes.get(AttributeKey.stringKey("stringKey")));
        assertEquals(data.get("longKey"), attributes.get(AttributeKey.longKey("longKey")));
        assertEquals(data.get("doubleKey"), attributes.get(AttributeKey.doubleKey("doubleKey")));
        assertEquals(data.get("booleanKey"), attributes.get(AttributeKey.booleanKey("booleanKey")));
    }
}
