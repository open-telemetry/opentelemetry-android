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

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

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

    @Test
    public void parsableHeader_singleQuotes() {
        ServerTimingHeaderParser parser = new ServerTimingHeaderParser();
        String traceId = "9499195c502eb217c448a68bfe0f967c";
        String spanId = "fe16eca542cd5d86";
        assertArrayEquals(new String[]{traceId, spanId},
                parser.parse("traceparent;desc='00-" + traceId + "-" + spanId + "-01'"));
    }
}