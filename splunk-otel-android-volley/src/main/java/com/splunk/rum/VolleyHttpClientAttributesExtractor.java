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

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.opentelemetry.instrumentation.api.instrumenter.http.CapturedHttpHeaders;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;

final class VolleyHttpClientAttributesExtractor extends HttpClientAttributesExtractor<RequestWrapper, HttpResponse> {

    VolleyHttpClientAttributesExtractor(CapturedHttpHeaders capturedHttpHeaders) {
        super(capturedHttpHeaders);
    }

    @Override
    protected String url(RequestWrapper requestWrapper) {
        return requestWrapper.getRequest().getUrl();
    }

    @Nullable
    @Override
    protected String flavor(RequestWrapper requestWrapper, @Nullable HttpResponse response) {
        return null;
    }

    @Override
    protected String method(RequestWrapper requestWrapper) {
        Request<?> request = requestWrapper.getRequest();
        switch (request.getMethod()) {
            case Request.Method.GET:
                return "GET";
            case Request.Method.POST:
                return "POST";
            case Request.Method.PUT:
                return "PUT";
            case Request.Method.DELETE:
                return "DELETE";
            case Request.Method.HEAD:
                return "HEAD";
            case Request.Method.OPTIONS:
                return "OPTIONS";
            case Request.Method.TRACE:
                return "TRACE";
            case Request.Method.PATCH:
                return "PATCH";
            default:
                return null;
        }
    }

    @Override
    protected List<String> requestHeader(RequestWrapper requestWrapper, String name) {
        String header;
        try {
            header = requestWrapper.getRequest().getHeaders().get(name);
            if (header == null) {
                header = requestWrapper.getAdditionalHeaders().get(name);
            }

        } catch (AuthFailureError e) {
            header = null;
        }

        return header != null ? Collections.singletonList(header) : Collections.emptyList();

    }

    @Nullable
    @Override
    protected Long requestContentLength(RequestWrapper requestWrapper, @Nullable HttpResponse response) {
        Request<?> request = requestWrapper.getRequest();
        try {
            return request.getBody() != null ? (long) request.getBody().length : null;
        } catch (AuthFailureError authFailureError) {
            return null;
        }
    }

    @Nullable
    @Override
    protected Long requestContentLengthUncompressed(RequestWrapper requestWrapper, @Nullable HttpResponse response) {
        return null;
    }

    @Override
    protected Integer statusCode(RequestWrapper requestWrapper, @Nullable HttpResponse response) {
        return response.getStatusCode();
    }

    @Override
    protected Long responseContentLength(RequestWrapper requestWrapper, @Nullable HttpResponse response) {
        return (long) response.getContentLength();
    }

    @Nullable
    @Override
    protected Long responseContentLengthUncompressed(RequestWrapper requestWrapper, @Nullable HttpResponse response) {
        return null;
    }

    @Override
    protected List<String> responseHeader(RequestWrapper requestWrapper, @Nullable HttpResponse response,
                                          String name) {
        return headersToList(response.getHeaders(), name);
    }

    static List<String> headersToList(List<Header> headers, String name) {
        if (headers.size() == 0) {
            return Collections.emptyList();
        }

        List<String> headersList = new ArrayList<>();
        for (Header header : headers) {
            if (header.getName().equals(name)) {
                headersList.add(header.getValue());
            }
        }
        return headersList;
    }
}
