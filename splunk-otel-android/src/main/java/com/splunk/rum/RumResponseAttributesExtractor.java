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

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import okhttp3.Request;
import okhttp3.Response;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

class RumResponseAttributesExtractor extends AttributesExtractor<Request, Response> {
    static final AttributeKey<String> LINK_TRACE_ID_KEY = stringKey("link.traceId");
    static final AttributeKey<String> LINK_SPAN_ID_KEY = stringKey("link.spanId");

    private final ServerTimingHeaderParser serverTimingHeaderParser;

    public RumResponseAttributesExtractor(ServerTimingHeaderParser serverTimingHeaderParser) {
        this.serverTimingHeaderParser = serverTimingHeaderParser;
    }

    @Override
    protected void onStart(AttributesBuilder attributes, Request request) {
        attributes.put(SplunkRum.COMPONENT_KEY, "http");
    }

    @Override
    protected void onEnd(AttributesBuilder attributes, Request request, Response response, Throwable error) {
        //null response means the call failed completely
        if (response == null) {
            return;
        }
        recordContentLength(attributes, response);
        String serverTimingHeader = response.header("Server-Timing");

        String[] ids = serverTimingHeaderParser.parse(serverTimingHeader);
        if (ids.length == 2) {
            attributes.put(LINK_TRACE_ID_KEY, ids[0]);
            attributes.put(LINK_SPAN_ID_KEY, ids[1]);
        }
    }

    private void recordContentLength(AttributesBuilder attributesBuilder, Response response) {
        //make a best low-impact effort at getting the content length on the response.
        String contentLengthHeader = response.header("Content-Length");
        if (contentLengthHeader != null) {
            try {
                long contentLength = Long.parseLong(contentLengthHeader);
                if (contentLength > 0) {
                    attributesBuilder.put(SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH, contentLength);
                }
            } catch (NumberFormatException e) {
                //who knows what we got back? It wasn't a number!
            }
        }
    }
}
