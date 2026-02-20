/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlConnectionSingletons.instrumenter
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlConnectionSingletons.openTelemetryInstance
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.RequestPropertySetter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URLConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

object HttpUrlReplacements {
    private val activeURLConnections: MutableMap<URLConnection, HttpURLConnectionInfo> =
        ConcurrentHashMap<URLConnection, HttpURLConnectionInfo>()

    private val logger: Logger = Logger.getLogger("HttpUrlReplacements")
    private const val UNKNOWN_RESPONSE_CODE: Int = -1

    private var httpURLInstrumenter: Instrumenter<URLConnection, Int>? = null

    @JvmStatic
    fun replacementForDisconnect(connection: HttpURLConnection) {
        // Ensure ending of un-ended spans while connection is still alive
        // If disconnect is not called, harvester thread if scheduled, takes care of ending any
        // un-ended spans.
        val info = activeURLConnections[connection]
        if (info != null && !info.reported) {
            reportWithResponseCode(connection)
        }

        connection.disconnect()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun replacementForConnect(connection: URLConnection) {
        startTracingAtFirstConnection(connection)

        try {
            connection.connect()
        } catch (exception: IOException) {
            reportWithThrowable(connection, exception)
            throw exception
        }

        updateLastSeenTime(connection)
        // connect() does not read anything from connection so request not harvestable yet (to be
        // reported if left idle).
    }

    @JvmStatic
    @Throws(IOException::class)
    fun replacementForContent(connection: URLConnection): Any = replaceThrowable(connection, connection::getContent)

    @Throws(IOException::class)
    fun replacementForContent(
        connection: URLConnection,
        classes: Array<Class<*>?>,
    ): Any = replaceThrowable(connection) { connection.getContent(classes) }

    @JvmStatic
    fun replacementForContentType(connection: URLConnection): String = replace(connection) { connection.contentType }

    @JvmStatic
    fun replacementForContentEncoding(connection: URLConnection): String = replace(connection) { connection.contentEncoding }

    @JvmStatic
    fun replacementForContentLength(connection: URLConnection): Int = replace(connection) { connection.contentLength }

    @JvmStatic
    fun replacementForContentLengthLong(connection: URLConnection): Long = replace(connection) { getContentLengthLong(connection) }

    @JvmStatic
    fun replacementForExpiration(connection: URLConnection): Long = replace(connection) { connection.expiration }

    @JvmStatic
    fun replacementForDate(connection: URLConnection): Long = replace(connection) { connection.date }

    @JvmStatic
    fun replacementForLastModified(connection: URLConnection): Long = replace(connection) { connection.lastModified }

    @JvmStatic
    fun replacementForHeaderField(
        connection: URLConnection,
        name: String,
    ): String = replace(connection) { connection.getHeaderField(name) }

    @JvmStatic
    fun replacementForHeaderFields(connection: URLConnection): MutableMap<String, MutableList<String>> =
        replace(connection) { connection.headerFields }

    @JvmStatic
    fun replacementForHeaderFieldInt(
        connection: URLConnection,
        name: String,
        default: Int,
    ): Int = replace(connection) { connection.getHeaderFieldInt(name, default) }

    @JvmStatic
    fun replacementForHeaderFieldLong(
        connection: URLConnection,
        name: String,
        default: Long,
    ): Long = replace(connection) { getHeaderFieldLong(connection, name, default) }

    @JvmStatic
    fun replacementForHeaderFieldDate(
        connection: URLConnection,
        name: String,
        default: Long,
    ): Long {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldDate method.
        return replace(connection) { connection.getHeaderFieldDate(name, default) }
    }

    @JvmStatic
    fun replacementForHttpHeaderFieldDate(
        connection: HttpURLConnection,
        name: String,
        default: Long,
    ): Long {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldDate
        // method.
        return replace(connection) { connection.getHeaderFieldDate(name, default) }
    }

    @JvmStatic
    fun replacementForHeaderFieldKey(
        connection: URLConnection,
        index: Int,
    ): String {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldKey method.
        return replace(connection) { connection.getHeaderFieldKey(index) }
    }

    @JvmStatic
    fun replacementForHttpHeaderFieldKey(
        connection: HttpURLConnection,
        index: Int,
    ): String {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldKey
        // method.
        return replace(connection) { connection.getHeaderFieldKey(index) }
    }

    @JvmStatic
    fun replacementForHeaderField(
        connection: URLConnection,
        index: Int,
    ): String {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderField method.
        return replace(connection) { connection.getHeaderField(index) }
    }

    @JvmStatic
    fun replacementForHttpHeaderField(
        connection: HttpURLConnection,
        index: Int,
    ): String {
        // URLConnection also overrides this and that is covered in replacementForHeaderField
        // method.
        return replace(connection) { connection.getHeaderField(index) }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun replacementForResponseCode(connection: URLConnection): Int {
        val httpURLConnection = connection as HttpURLConnection
        return replaceThrowable(connection, httpURLConnection::getResponseCode)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun replacementForResponseMessage(connection: URLConnection): String {
        val httpURLConnection = connection as HttpURLConnection
        return replaceThrowable(connection, httpURLConnection::getResponseMessage)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun replacementForOutputStream(connection: URLConnection): OutputStream =
        replaceThrowable(
            connection,
            { connection.getOutputStream() },
            false,
        )

    @JvmStatic
    @Throws(IOException::class)
    fun replacementForInputStream(connection: URLConnection): InputStream? {
        startTracingAtFirstConnection(connection)

        val inputStream: InputStream?
        try {
            inputStream = connection.getInputStream()
        } catch (exception: IOException) {
            reportWithThrowable(connection, exception)
            throw exception
        }

        if (inputStream == null) {
            return inputStream
        }

        return InstrumentedInputStream(connection, inputStream)
    }

    @JvmStatic
    fun replacementForErrorStream(connection: HttpURLConnection): InputStream? {
        startTracingAtFirstConnection(connection)

        val errorStream = connection.errorStream

        if (errorStream == null) {
            return errorStream
        }

        return InstrumentedInputStream(connection, errorStream)
    }

    private fun <T> replace(
        connection: URLConnection,
        resultProvider: () -> T,
    ): T {
        startTracingAtFirstConnection(connection)

        val result: T = resultProvider()

        updateLastSeenTime(connection)
        markHarvestable(connection)

        return result
    }

    @Throws(IOException::class)
    private inline fun <T> replaceThrowable(
        connection: URLConnection,
        resultProvider: () -> T,
    ): T = replaceThrowable(connection, resultProvider, true)

    @Throws(IOException::class)
    private inline fun <T> replaceThrowable(
        connection: URLConnection,
        resultProvider: () -> T,
        shouldMarkHarvestable: Boolean,
    ): T {
        startTracingAtFirstConnection(connection)

        val result: T
        try {
            result = resultProvider()
        } catch (exception: IOException) {
            reportWithThrowable(connection, exception)
            throw exception
        }

        updateLastSeenTime(connection)
        if (shouldMarkHarvestable) {
            markHarvestable(connection)
        }

        return result
    }

    private fun reportWithThrowable(
        connection: URLConnection,
        exception: IOException,
    ) {
        endTracing(connection, UNKNOWN_RESPONSE_CODE, exception)
    }

    private fun reportWithResponseCode(connection: HttpURLConnection) {
        try {
            endTracing(connection, connection.getResponseCode(), null)
        } catch (exception: IOException) {
            logger.log(
                Level.FINE,
                (
                    "Exception " +
                        exception.message +
                        " was thrown while ending span for connection " +
                        connection
                ),
            )
        }
    }

    private fun endTracing(
        connection: URLConnection,
        responseCode: Int,
        error: Throwable?,
    ) {
        val info = activeURLConnections[connection]
        if (info != null && !info.reported) {
            val context = info.context
            httpURLInstrumenter?.end(context, connection, responseCode, error)
            info.reported = true
            activeURLConnections.remove(connection)
        }
    }

    private fun startTracingAtFirstConnection(connection: URLConnection) {
        val parentContext = Context.current()
        val instrument = instrumenter()
        httpURLInstrumenter = instrument
        if (!instrument.shouldStart(parentContext, connection)) {
            return
        }

        if (!activeURLConnections.containsKey(connection)) {
            val context = httpURLInstrumenter?.start(parentContext, connection) ?: return
            activeURLConnections[connection] = HttpURLConnectionInfo(context)
            try {
                injectContextToRequest(connection, context)
            } catch (exception: Exception) {
                // If connection was already made prior to setting this request property,
                // (which should not happen as we've instrumented all methods that connect)
                // above call would throw IllegalStateException.
                logger.log(
                    Level.FINE,
                    (
                        "Exception " +
                            exception.message +
                            " was thrown while adding distributed tracing context for connection " +
                            connection
                    ),
                    exception,
                )
            }
        }
    }

    private fun injectContextToRequest(
        connection: URLConnection,
        context: Context,
    ) {
        openTelemetryInstance()
            .propagators
            .textMapPropagator
            .inject(context, connection, RequestPropertySetter)
    }

    private fun updateLastSeenTime(connection: URLConnection) {
        val info = activeURLConnections[connection]
        if (info != null && !info.reported) {
            info.lastSeenTime = SystemClock.uptimeMillis()
        }
    }

    private fun markHarvestable(connection: URLConnection) {
        val info = activeURLConnections[connection]
        if (info != null && !info.reported) {
            info.harvestable = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getContentLengthLong(connection: URLConnection): Long = connection.contentLengthLong

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getHeaderFieldLong(
        connection: URLConnection,
        name: String,
        default: Long,
    ): Long = connection.getHeaderFieldLong(name, default)

    fun reportIdleConnectionsOlderThan(timeInterval: Long) {
        val timeNow = SystemClock.uptimeMillis()
        for (connection in activeURLConnections.keys) {
            val info = activeURLConnections[connection]
            if (info != null && info.harvestable &&
                !info.reported && (info.lastSeenTime + timeInterval) < timeNow
            ) {
                val httpURLConnection = connection as HttpURLConnection
                reportWithResponseCode(httpURLConnection)
            }
        }
    }

    internal class HttpURLConnectionInfo(
        val context: Context,
    ) {
        var lastSeenTime: Long = SystemClock.uptimeMillis()
        var reported = false
        var harvestable = false
    }

    private class InstrumentedInputStream(
        private val connection: URLConnection,
        private val inputStream: InputStream,
    ) : InputStream() {
        @Throws(IOException::class)
        override fun read(): Int {
            val res: Int
            try {
                res = inputStream.read()
            } catch (exception: IOException) {
                reportWithThrowable(connection, exception)
                throw exception
            }
            reportIfDoneOrMarkHarvestable(res)
            return res
        }

        @Throws(IOException::class)
        override fun read(b: ByteArray): Int {
            val res: Int
            try {
                res = inputStream.read(b)
            } catch (exception: IOException) {
                reportWithThrowable(connection, exception)
                throw exception
            }
            reportIfDoneOrMarkHarvestable(res)
            return res
        }

        @Throws(IOException::class)
        override fun read(
            b: ByteArray,
            off: Int,
            len: Int,
        ): Int {
            val res: Int
            try {
                res = inputStream.read(b, off, len)
            } catch (exception: IOException) {
                reportWithThrowable(connection, exception)
                throw exception
            }
            reportIfDoneOrMarkHarvestable(res)
            return res
        }

        @Throws(IOException::class)
        override fun close() {
            val httpURLConnection = connection as HttpURLConnection
            reportWithResponseCode(httpURLConnection)
            inputStream.close()
        }

        fun reportIfDoneOrMarkHarvestable(result: Int) {
            if (result == -1) {
                val httpURLConnection = connection as HttpURLConnection
                reportWithResponseCode(httpURLConnection)
            } else {
                markHarvestable(connection)
            }
        }
    }
}
