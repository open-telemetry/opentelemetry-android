/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import android.os.SystemClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlConnectionSingletons
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlHttpAttributesGetter
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

internal class HttpUrlReplacementsTest {
    companion object {
        @JvmField
        @RegisterExtension
        val otelTesting: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.uptimeMillis() } returns 1000L
        HttpUrlConnectionSingletons.configure(HttpUrlInstrumentation(), otelTesting.openTelemetry)
    }

    @AfterEach
    fun tearDown() {
        HttpUrlConnectionSingletons.reset()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `replacementForInputStream returns stream directly and closes safely for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>(relaxed = true)
        val bytes = "hello".toByteArray()
        val inputStream = ByteArrayInputStream(bytes)
        every { connection.getInputStream() } returns inputStream

        val resultStream: InputStream? = HttpUrlReplacements.replacementForInputStream(connection)

        assertThat(resultStream).isNotNull
        assertThat(resultStream?.readBytes()).isEqualTo(bytes)
        assertThatCode { resultStream?.close() }.doesNotThrowAnyException()
        verify(exactly = 0) { connection.setRequestProperty(any(), any()) }
        assertThat(otelTesting.spans).isEmpty()
    }

    @Test
    fun `replacementForResponseCode returns unknown for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>(relaxed = true)

        val code = HttpUrlReplacements.replacementForResponseCode(connection)

        assertThat(code).isEqualTo(-1)
        verify(exactly = 0) { connection.setRequestProperty(any(), any()) }
        assertThat(otelTesting.spans).isEmpty()
    }

    @Test
    fun `replacementForResponseMessage returns empty string for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>(relaxed = true)

        val message = HttpUrlReplacements.replacementForResponseMessage(connection)

        assertThat(message).isEmpty()
        verify(exactly = 0) { connection.setRequestProperty(any(), any()) }
        assertThat(otelTesting.spans).isEmpty()
    }

    @Test
    fun `replacementForConnect does not fail for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>(relaxed = true)

        assertThatCode { HttpUrlReplacements.replacementForConnect(connection) }.doesNotThrowAnyException()
        verify { connection.connect() }
        verify(exactly = 0) { connection.setRequestProperty(any(), any()) }
        assertThat(otelTesting.spans).isEmpty()
    }

    @Test
    fun `replacementForOutputStream does not fail for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>(relaxed = true)
        val outputStream = ByteArrayOutputStream()
        every { connection.getOutputStream() } returns outputStream

        val result = HttpUrlReplacements.replacementForOutputStream(connection)

        assertThat(result).isSameAs(outputStream)
        verify(exactly = 0) { connection.setRequestProperty(any(), any()) }
        assertThat(otelTesting.spans).isEmpty()
    }

    @Test
    fun `reportIdleConnectionsOlderThan safely ignores non-HttpURLConnection`() {
        assertThatCode {
            HttpUrlReplacements.reportIdleConnectionsOlderThan(0)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `HttpUrlHttpAttributesGetter returns null request method for non-HttpURLConnection`() {
        val getter = HttpUrlHttpAttributesGetter()
        val connection = mockk<URLConnection>()

        assertThat(getter.getHttpRequestMethod(connection)).isNull()
    }

    @Test
    fun `HttpURLConnection control confirms tracing is active and records span`() {
        val httpConnection = mockk<HttpURLConnection>(relaxed = true)
        every { httpConnection.url } returns URL("http://localhost/test")
        every { httpConnection.requestMethod } returns "GET"
        every { httpConnection.responseCode } returns 200
        val httpStream = ByteArrayInputStream("http response".toByteArray())
        every { httpConnection.getInputStream() } returns httpStream

        val httpResultStream = HttpUrlReplacements.replacementForInputStream(httpConnection)
        verify { httpConnection.setRequestProperty("traceparent", any()) }

        httpResultStream?.close()
        assertThat(otelTesting.spans).hasSize(1)
        assertThat(otelTesting.spans[0].name).isEqualTo("GET")
    }
}
