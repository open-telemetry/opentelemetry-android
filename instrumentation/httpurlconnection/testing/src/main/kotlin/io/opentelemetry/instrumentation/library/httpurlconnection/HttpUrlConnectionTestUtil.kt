/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object HttpUrlConnectionTestUtil {
    private const val TAG = "HttpUrlConnectionTest"

    fun executeGet(
        inputUrl: String,
        getInputStream: Boolean = true,
        disconnect: Boolean = true,
        onComplete: Runnable = Runnable {},
    ) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(inputUrl).openConnection() as HttpURLConnection

            // always call one API that reads from the connection
            val responseCode = connection.responseCode

            val readInput = if (getInputStream) connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() } else ""

            Log.d(TAG, "response code: $responseCode ,input Stream: $readInput")
        } catch (e: IOException) {
            Log.e(TAG, "Exception occurred while executing GET request", e)
        } finally {
            connection?.takeIf { disconnect }?.disconnect()
            onComplete.run()
        }
    }

    fun post(inputUrl: String) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(inputUrl).openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.requestMethod = "POST"

            connection.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { out -> out.write("Writing content to output stream!") }

            // always call one API that reads from the connection
            val readInput = connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }

            Log.d(TAG, "InputStream: $readInput")
        } catch (e: IOException) {
            Log.e(TAG, "Exception occurred while executing post", e)
        } finally {
            connection?.disconnect()
        }
    }
}
