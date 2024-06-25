/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection;

import android.util.Log;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUrlConnectionTestUtil {
    private static final String TAG = "HttpUrlConnectionTest";

    public static void executeGet(String url) {
        executeCustomGet(url, true, true);
    }

    public static void executeCustomGet(
            String inputUrl, boolean getInputStream, boolean disconnect) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(inputUrl);
            connection = (HttpURLConnection) url.openConnection();

            // always call one API that reads from the connection
            int responseCode = connection.getResponseCode();

            String readInput = getInputStream ? readInputStream(connection) : "";

            Log.d(TAG, "response code: " + responseCode + " ,input Stream: " + readInput);

        } catch (IOException e) {
            Log.e(TAG, "Exception occured while executing GET request", e);
        } finally {
            if (connection != null && disconnect) {
                connection.disconnect();
            }
        }
    }

    public static void post(String inputUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(inputUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            writeToOutputStream(connection, "Writing content to output stream!");

            // always call one API that reads from the connection
            String readInput = readInputStream(connection);

            Log.d(TAG, "InputStream: " + readInput);

        } catch (IOException e) {
            Log.e(TAG, "Exception occured while executing post" + e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void writeToOutputStream(HttpURLConnection connection, String string)
            throws IOException {
        BufferedWriter writer =
                new BufferedWriter(
                        new OutputStreamWriter(
                                connection.getOutputStream(), StandardCharsets.UTF_8));
        writer.write(string);
        writer.close();
    }

    private static String readInputStream(HttpURLConnection connection) throws IOException {
        String readInput;
        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
        }
        readInput = stringBuilder.toString();
        reader.close();
        return readInput;
    }
}
