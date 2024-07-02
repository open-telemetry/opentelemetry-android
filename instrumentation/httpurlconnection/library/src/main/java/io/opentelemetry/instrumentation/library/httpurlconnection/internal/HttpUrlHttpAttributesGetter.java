/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection.internal;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;
import javax.annotation.Nullable;

public class HttpUrlHttpAttributesGetter
        implements HttpClientAttributesGetter<URLConnection, Integer> {

    @Override
    public String getHttpRequestMethod(URLConnection connection) {
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        return httpURLConnection.getRequestMethod();
    }

    @Override
    public String getUrlFull(URLConnection connection) {
        return connection.getURL().toExternalForm();
    }

    @Override
    public List<String> getHttpRequestHeader(URLConnection connection, String name) {
        String value = connection.getRequestProperty(name);
        return value == null ? emptyList() : singletonList(value);
    }

    @Override
    public Integer getHttpResponseStatusCode(
            URLConnection connection, Integer statusCode, @Nullable Throwable error) {
        return statusCode;
    }

    @Override
    public List<String> getHttpResponseHeader(
            URLConnection connection, Integer statusCode, String name) {
        String value = connection.getHeaderField(name);
        return value == null ? emptyList() : singletonList(value);
    }

    @Nullable
    @Override
    public String getNetworkProtocolName(URLConnection connection, @Nullable Integer integer) {
        // HttpURLConnection hardcodes the protocol name&version
        return "http";
    }

    @Nullable
    @Override
    public String getNetworkProtocolVersion(URLConnection connection, @Nullable Integer integer) {
        // HttpURLConnection hardcodes the protocol name&version
        return "1.1";
    }

    @Override
    public String getServerAddress(URLConnection connection) {
        return connection.getURL().getHost();
    }

    @Override
    public Integer getServerPort(URLConnection connection) {
        return connection.getURL().getPort();
    }
}
