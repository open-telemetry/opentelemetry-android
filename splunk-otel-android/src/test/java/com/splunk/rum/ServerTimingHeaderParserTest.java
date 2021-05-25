package com.splunk.rum;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

//the header looks like: traceparent;desc="00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01"
public class ServerTimingHeaderParserTest {

    @Test
    public void badHeader() {
        ServerTimingHeaderParser parser = new ServerTimingHeaderParser();
        assertArrayEquals(new String[0], parser.parse(null));
        assertArrayEquals(new String[0], parser.parse("foo"));
        assertArrayEquals(new String[0], parser.parse("traceparent;gotcha"));
        assertArrayEquals(new String[0], parser.parse("traceparent;desc=\"\""));
        assertArrayEquals(new String[0], parser.parse("traceparent;desc=\"x-\""));
        assertArrayEquals(new String[0], parser.parse("traceparent;desc=\"-\""));
        assertArrayEquals(new String[0], parser.parse("traceparent;desc=\"--\""));
        assertArrayEquals(new String[0], parser.parse("traceparent;desc=\"00-abc-123\""));
    }

    @Test
    public void parsableHeader() {
        ServerTimingHeaderParser parser = new ServerTimingHeaderParser();
        String traceId = "9499195c502eb217c448a68bfe0f967c";
        String spanId = "fe16eca542cd5d86";
        assertArrayEquals(new String[]{traceId, spanId},
                parser.parse("traceparent;desc=\"00-" + traceId + "-" + spanId + "-01\""));
    }
}