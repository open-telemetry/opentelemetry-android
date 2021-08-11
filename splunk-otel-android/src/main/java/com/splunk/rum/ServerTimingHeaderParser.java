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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ServerTimingHeaderParser {

    private static final String[] UNPARSEABLE_RESULT = new String[0];

    private static final Pattern headerPattern = Pattern.compile("traceparent;desc=['\"]00-([0-9a-f]{32})-([0-9a-f]{16})-01['\"]");

    /**
     * The first element is the trace id, the 2nd is the span id.
     *
     * @param header of the form: traceparent;desc="00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01"
     * @return A two-element array of TraceId/SpanId. An empty array if the header can't be parsed.
     *
     * This will also consider single-quotes valid for delimiting the "desc" section, even though it's not to spec.
     */
    String[] parse(String header) {
        if (header == null) {
            return UNPARSEABLE_RESULT;
        }
        Matcher matcher = headerPattern.matcher(header);
        if (!matcher.matches()) {
            return UNPARSEABLE_RESULT;
        }
        String traceId = matcher.group(1);
        String spanId = matcher.group(2);
        return new String[]{traceId, spanId};
    }

}
