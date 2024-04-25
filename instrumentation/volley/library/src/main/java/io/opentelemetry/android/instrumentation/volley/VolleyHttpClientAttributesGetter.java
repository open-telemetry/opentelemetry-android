/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.volley;

import static java.util.Collections.emptyList;

import android.net.Uri;

import androidx.annotation.Nullable;
import com.android.volley.AuthFailureError;
import com.android.volley.Header;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpResponse;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

enum VolleyHttpClientAttributesGetter
        implements HttpClientAttributesGetter<RequestWrapper, HttpResponse> {
    INSTANCE;

    @Override
    @Nullable
    public String getUrlFull(RequestWrapper requestWrapper) {
        return requestWrapper.getRequest().getUrl();
    }

    @Nullable
    @Override
    public String getServerAddress(RequestWrapper requestWrapper) {
        return Uri.parse(requestWrapper.getRequest().getUrl()).getHost();
    }

    @Nullable
    @Override
    public Integer getServerPort(RequestWrapper requestWrapper) {
        return Uri.parse(requestWrapper.getRequest().getUrl()).getPort();
    }

    @Nullable
    @Override
    public String getHttpRequestMethod(RequestWrapper requestWrapper) {
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
    public List<String> getHttpRequestHeader(RequestWrapper requestWrapper, String name) {
        Request<?> request = requestWrapper.getRequest();
        try {
            Map<String, String> headers = request.getHeaders();
            Map<String, String> additionalHeaders = requestWrapper.getAdditionalHeaders();
            List<String> result = new ArrayList<>();
            result.addAll(findCaseInsensitive(name, headers));
            result.addAll(findCaseInsensitive(name, additionalHeaders));
            return result;
        } catch (AuthFailureError e) {
            return emptyList();
        }
    }

    private List<String> findCaseInsensitive(String name, Map<String, String> headers) {
        List<String> result = new ArrayList<>(headers.size());
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getKey().equalsIgnoreCase(name)) {
                result.add(header.getValue());
            }
        }
        return result;
    }

    @Override
    public Integer getHttpResponseStatusCode(
            RequestWrapper requestWrapper, HttpResponse response, @Nullable Throwable error) {
        return response.getStatusCode();
    }

    @Override
    public List<String> getHttpResponseHeader(
            RequestWrapper requestWrapper, @Nullable HttpResponse response, String name) {
        if (response == null) {
            return emptyList();
        }
        return headersToList(response.getHeaders(), name);
    }

    static List<String> headersToList(List<Header> headers, String name) {
        if (headers.size() == 0) {
            return emptyList();
        }

        List<String> headersList = new ArrayList<>();
        for (Header header : headers) {
            if (header.getName().equalsIgnoreCase(name)) {
                headersList.add(header.getValue());
            }
        }
        return headersList;
    }

    @Override
    public String getNetworkTransport(RequestWrapper requestWrapper, HttpResponse httpResponse) {
        return "tcp";
    }
}
