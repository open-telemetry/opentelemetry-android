package com.splunk.rum;

import static com.splunk.rum.SplunkRum.LINK_SPAN_ID_KEY;
import static com.splunk.rum.SplunkRum.LINK_TRACE_ID_KEY;

import androidx.annotation.Nullable;

import com.android.volley.Header;
import com.android.volley.toolbox.HttpResponse;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

class VolleyResponseAttributesExtractor implements AttributesExtractor<RequestWrapper, HttpResponse> {

    private final ServerTimingHeaderParser serverTimingHeaderParser;

    public VolleyResponseAttributesExtractor(ServerTimingHeaderParser serverTimingHeaderParser) {
        this.serverTimingHeaderParser = serverTimingHeaderParser;
    }

    @Override
    public void onStart(AttributesBuilder attributes, Context parentContext, RequestWrapper requestWrapper) {
        attributes.put(SplunkRum.COMPONENT_KEY, "http");
    }

    @Override
    public void onEnd(AttributesBuilder attributes, Context context, RequestWrapper requestWrapper, @Nullable HttpResponse response, @Nullable Throwable error) {
        if (response != null) {
            onResponse(attributes, response);
        }
    }


    private void onResponse(AttributesBuilder attributes, HttpResponse response) {
        String serverTimingHeader = getHeader(response, "Server-Timing");

        String[] ids = serverTimingHeaderParser.parse(serverTimingHeader);
        if (ids.length == 2) {
            attributes.put(LINK_TRACE_ID_KEY, ids[0]);
            attributes.put(LINK_SPAN_ID_KEY, ids[1]);
        }
    }

    private String getHeader(HttpResponse response, String headerName) {
        for (Header header : response.getHeaders()) {
            if (header.getName().equals(headerName)) {
                return header.getValue();
            }
        }
        return null;
    }
}
