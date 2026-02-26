/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection.internal

import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter
import java.net.HttpURLConnection
import java.net.URLConnection

internal class HttpUrlHttpAttributesGetter : HttpClientAttributesGetter<URLConnection, Int> {
    override fun getHttpRequestMethod(connection: URLConnection): String {
        val httpURLConnection = connection as HttpURLConnection
        return httpURLConnection.requestMethod
    }

    override fun getUrlFull(connection: URLConnection): String = connection.getURL().toExternalForm()

    override fun getHttpRequestHeader(
        connection: URLConnection,
        name: String,
    ): MutableList<String?> =
        when (val value = connection.getRequestProperty(name)) {
            null -> mutableListOf()
            else -> mutableListOf(value)
        }

    override fun getHttpResponseStatusCode(
        connection: URLConnection,
        statusCode: Int,
        error: Throwable?,
    ): Int = statusCode

    override fun getHttpResponseHeader(
        connection: URLConnection,
        statusCode: Int,
        name: String,
    ): MutableList<String?> =
        when (val value = connection.getHeaderField(name)) {
            null -> mutableListOf()
            else -> mutableListOf(value)
        }

    // HttpURLConnection hardcodes the protocol name&version
    override fun getNetworkProtocolName(
        connection: URLConnection,
        integer: Int?,
    ): String = "http"

    // HttpURLConnection hardcodes the protocol name&version
    override fun getNetworkProtocolVersion(
        connection: URLConnection,
        integer: Int?,
    ): String = "1.1"

    override fun getServerAddress(connection: URLConnection): String = connection.getURL().host

    override fun getServerPort(connection: URLConnection): Int = connection.getURL().port
}
