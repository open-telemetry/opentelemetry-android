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

import static com.splunk.rum.SplunkRum.LINK_SPAN_ID_KEY;
import static com.splunk.rum.SplunkRum.LINK_TRACE_ID_KEY;

import androidx.annotation.Nullable;
import com.android.volley.Header;
import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

class VolleyResponseAttributesExtractor
        implements AttributesExtractor<RequestWrapper, HttpResponse> {

    private final ServerTimingHeaderParser serverTimingHeaderParser;

    public VolleyResponseAttributesExtractor(ServerTimingHeaderParser serverTimingHeaderParser) {
        this.serverTimingHeaderParser = serverTimingHeaderParser;
    }

    @Override
    public void onStart(
            AttributesBuilder attributes, Context parentContext, RequestWrapper requestWrapper) {
        attributes.put(SplunkRum.COMPONENT_KEY, "http");
    }

    @Override
    public void onEnd(
            AttributesBuilder attributes,
            Context context,
            RequestWrapper requestWrapper,
            @Nullable HttpResponse response,
            @Nullable Throwable error) {
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

    @Nullable
    private String getHeader(HttpResponse response, String headerName) {
        for (Header header : response.getHeaders()) {
            if (header.getName().equals(headerName)) {
                return header.getValue();
            }
        }
        return null;
    }
}
