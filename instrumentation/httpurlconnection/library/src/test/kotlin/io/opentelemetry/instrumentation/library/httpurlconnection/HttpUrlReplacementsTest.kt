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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLConnection

internal class HttpUrlReplacementsTest {
    @AfterEach
    fun tearDown() {
        HttpUrlConnectionSingletons.reset()
    }

    @Test
    fun `replacementForInputStream returns stream directly and closes safely for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>()
        val bytes = "hello".toByteArray()
        val inputStream = ByteArrayInputStream(bytes)
        every { connection.getInputStream() } returns inputStream

        val resultStream: InputStream? = HttpUrlReplacements.replacementForInputStream(connection)

        assertThat(resultStream).isNotNull
        assertThat(resultStream?.readBytes()).isEqualTo(bytes)
        assertThatCode { resultStream?.close() }.doesNotThrowAnyException()
    }

    @Test
    fun `replacementForResponseCode returns unknown for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>()

        val code = HttpUrlReplacements.replacementForResponseCode(connection)

        assertThat(code).isEqualTo(-1)
    }

    @Test
    fun `replacementForResponseMessage returns empty string for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>()

        val message = HttpUrlReplacements.replacementForResponseMessage(connection)

        assertThat(message).isEmpty()
    }

    @Test
    fun `replacementForConnect does not fail for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>(relaxed = true)

        assertThatCode { HttpUrlReplacements.replacementForConnect(connection) }.doesNotThrowAnyException()
        verify { connection.connect() }
    }

    @Test
    fun `replacementForOutputStream does not fail for non-HttpURLConnection`() {
        val connection = mockk<URLConnection>()
        val outputStream = ByteArrayOutputStream()
        every { connection.getOutputStream() } returns outputStream

        val result = HttpUrlReplacements.replacementForOutputStream(connection)

        assertThat(result).isSameAs(outputStream)
    }

    @Test
    fun `reportIdleConnectionsOlderThan safely ignores non-HttpURLConnection`() {
        mockkStatic(SystemClock::class)
        every { SystemClock.uptimeMillis() } returns 1000L
        try {
            assertThatCode {
                HttpUrlReplacements.reportIdleConnectionsOlderThan(0)
            }.doesNotThrowAnyException()
        } finally {
            unmockkStatic(SystemClock::class)
        }
    }

    @Test
    fun `HttpUrlHttpAttributesGetter returns null request method for non-HttpURLConnection`() {
        val getter = HttpUrlHttpAttributesGetter()
        val connection = mockk<URLConnection>()

        assertThat(getter.getHttpRequestMethod(connection)).isNull()
    }
}
