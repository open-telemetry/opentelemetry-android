/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.smoketest

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

internal data class CapturedHttpRequest(
    val method: String,
    val target: String,
    val headers: Map<String, String>,
    val body: ByteArray,
)

internal class OtlpHttpServer : Closeable {
    private val events = LinkedBlockingQueue<Result<CapturedHttpRequest>>()
    private val serverSocket = ServerSocket(0, 1, InetAddress.getByName(LOOPBACK_ADDRESS))
    private val serverThread =
        Thread(::serve, "otel-smoke-test-http").apply {
            isDaemon = true
            start()
        }

    val url = "http://$LOOPBACK_ADDRESS:${serverSocket.localPort}/"

    fun takeRequest(timeoutNanos: Long): CapturedHttpRequest? {
        val event = events.poll(timeoutNanos, TimeUnit.NANOSECONDS) ?: return null
        return event.getOrElse { failure ->
            throw AssertionError("Local OTLP server failed").apply { initCause(failure) }
        }
    }

    override fun close() {
        serverSocket.close()
        serverThread.join(SERVER_SHUTDOWN_TIMEOUT_MILLIS)
        check(!serverThread.isAlive) { "Local OTLP server did not stop" }
    }

    private fun serve() {
        while (!serverSocket.isClosed) {
            try {
                serverSocket.accept().use { socket ->
                    val request = readRequest(socket)
                    writeResponse(socket)
                    events.put(Result.success(request))
                }
            } catch (failure: Exception) {
                if (!serverSocket.isClosed) {
                    events.offer(Result.failure(failure))
                }
                return
            }
        }
    }

    private fun readRequest(socket: Socket): CapturedHttpRequest {
        socket.soTimeout = SOCKET_TIMEOUT_MILLIS
        val input = BufferedInputStream(socket.getInputStream())
        val requestParts = readLine(input).split(' ', limit = 3)
        require(requestParts.size == 3) { "Malformed HTTP request line" }

        val headers = linkedMapOf<String, String>()
        repeat(MAX_HEADER_COUNT) {
            val line = readLine(input)
            if (line.isEmpty()) {
                return CapturedHttpRequest(
                    requestParts[0],
                    requestParts[1],
                    headers,
                    readBody(input, headers),
                )
            }
            val separator = line.indexOf(':')
            require(separator > 0) { "Malformed HTTP header" }
            val name = line.substring(0, separator).lowercase(Locale.US)
            val value = line.substring(separator + 1).trim()
            headers[name] = value
        }
        error("Too many HTTP headers")
    }

    private fun readBody(
        input: InputStream,
        headers: Map<String, String>,
    ): ByteArray =
        if (headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true) {
            readChunkedBody(input)
        } else {
            val length = headers["content-length"]?.toIntOrNull() ?: 0
            require(length in 0..MAX_BODY_BYTES) { "Invalid HTTP content length: $length" }
            readBytes(input, length)
        }

    private fun readChunkedBody(input: InputStream): ByteArray {
        val body = ByteArrayOutputStream()
        while (true) {
            val size = readLine(input).substringBefore(';').trim().toIntOrNull(16)
            require(size != null && size >= 0 && size <= MAX_BODY_BYTES - body.size()) {
                "Invalid HTTP chunk size"
            }
            if (size == 0) {
                require(readLine(input).isEmpty()) { "Unexpected HTTP trailer headers" }
                return body.toByteArray()
            }
            body.write(readBytes(input, size))
            require(input.read() == '\r'.code && input.read() == '\n'.code) {
                "Malformed HTTP chunk terminator"
            }
        }
    }

    private fun readBytes(
        input: InputStream,
        size: Int,
    ): ByteArray = ByteArray(size).also { DataInputStream(input).readFully(it) }

    private fun readLine(input: InputStream): String {
        val line = StringBuilder()
        repeat(MAX_LINE_BYTES) {
            val next = input.read()
            require(next >= 0) { "Unexpected end of HTTP headers" }
            if (next == '\n'.code) {
                return line.toString().removeSuffix("\r")
            }
            line.append(next.toChar())
        }
        error("HTTP header line is too long")
    }

    private fun writeResponse(socket: Socket) {
        val output = socket.getOutputStream()
        output.write("HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
        output.flush()
    }

    private companion object {
        const val LOOPBACK_ADDRESS = "127.0.0.1"
        const val MAX_BODY_BYTES = 1024 * 1024
        const val MAX_LINE_BYTES = 8 * 1024
        const val MAX_HEADER_COUNT = 100
        const val SOCKET_TIMEOUT_MILLIS = 10_000
        const val SERVER_SHUTDOWN_TIMEOUT_MILLIS = 1_000L
    }
}
