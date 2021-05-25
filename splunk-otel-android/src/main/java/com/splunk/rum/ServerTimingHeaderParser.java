package com.splunk.rum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ServerTimingHeaderParser {

    private static final String[] UNPARSEABLE_RESULT = new String[0];

    private static final Pattern headerPattern = Pattern.compile("traceparent;desc=\"00-([0-9a-f]{32})-([0-9a-f]{16})-01\"");

    /**
     * The first element is the trace id, the 2nd is the span id.
     *
     * @param header of the form: traceparent;desc="00-9499195c502eb217c448a68bfe0f967c-fe16eca542cd5d86-01"
     * @return A two-element array of TraceId/SpanId. An empty array if the header can't be parsed.
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
