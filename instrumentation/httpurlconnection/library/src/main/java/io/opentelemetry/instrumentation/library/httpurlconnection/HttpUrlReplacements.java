/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection;

import android.os.Build;
import android.os.SystemClock;
import androidx.annotation.RequiresApi;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlConnectionSingletons;
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.RequestPropertySetter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpUrlReplacements {

    private static final Map<URLConnection, HttpURLConnectionInfo> activeURLConnections =
            new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger("HttpUrlReplacements");
    public static final int UNKNOWN_RESPONSE_CODE = -1;

    public static void replacementForDisconnect(HttpURLConnection connection) {
        // Ensure ending of un-ended spans while connection is still alive
        // If disconnect is not called, harvester thread if scheduled, takes care of ending any
        // un-ended spans.
        final HttpURLConnectionInfo info = activeURLConnections.get(connection);
        if (info != null && !info.reported) {
            reportWithResponseCode(connection);
        }

        connection.disconnect();
    }

    public static void replacementForConnect(URLConnection connection) throws IOException {
        startTracingAtFirstConnection(connection);

        try {
            connection.connect();
        } catch (IOException exception) {
            reportWithThrowable(connection, exception);
            throw exception;
        }

        updateLastSeenTime(connection);
        // connect() does not read anything from connection so request not harvestable yet (to be
        // reported if left idle).
    }

    public static Object replacementForContent(URLConnection connection) throws IOException {
        return replaceThrowable(connection, connection::getContent);
    }

    public static Object replacementForContent(URLConnection connection, Class<?>[] classes)
            throws IOException {
        return replaceThrowable(connection, () -> connection.getContent(classes));
    }

    public static String replacementForContentType(URLConnection connection) {
        return replace(connection, () -> connection.getContentType());
    }

    public static String replacementForContentEncoding(URLConnection connection) {
        return replace(connection, () -> connection.getContentEncoding());
    }

    public static int replacementForContentLength(URLConnection connection) {
        return replace(connection, () -> connection.getContentLength());
    }

    public static long replacementForContentLengthLong(URLConnection connection) {
        return replace(
                connection,
                new ResultProvider<Long>() {
                    @RequiresApi(Build.VERSION_CODES.N)
                    @Override
                    public Long get() {
                        return connection.getContentLengthLong();
                    }
                });
    }

    public static long replacementForExpiration(URLConnection connection) {
        return replace(connection, () -> connection.getExpiration());
    }

    public static long replacementForDate(URLConnection connection) {
        return replace(connection, () -> connection.getDate());
    }

    public static long replacementForLastModified(URLConnection connection) {
        return replace(connection, () -> connection.getLastModified());
    }

    public static String replacementForHeaderField(URLConnection connection, String name) {
        return replace(connection, () -> connection.getHeaderField(name));
    }

    public static Map<String, List<String>> replacementForHeaderFields(URLConnection connection) {
        return replace(connection, () -> connection.getHeaderFields());
    }

    public static int replacementForHeaderFieldInt(
            URLConnection connection, String name, int Default) {
        return replace(connection, () -> connection.getHeaderFieldInt(name, Default));
    }

    public static long replacementForHeaderFieldLong(
            URLConnection connection, String name, long Default) {
        return replace(
                connection,
                new ResultProvider<Long>() {
                    @RequiresApi(Build.VERSION_CODES.N)
                    @Override
                    public Long get() {
                        return connection.getHeaderFieldLong(name, Default);
                    }
                });
    }

    public static long replacementForHeaderFieldDate(
            URLConnection connection, String name, long Default) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldDate method.
        return replace(connection, () -> connection.getHeaderFieldDate(name, Default));
    }

    public static long replacementForHttpHeaderFieldDate(
            HttpURLConnection connection, String name, long Default) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldDate
        // method.
        return replace(connection, () -> connection.getHeaderFieldDate(name, Default));
    }

    public static String replacementForHeaderFieldKey(URLConnection connection, int index) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldKey method.
        return replace(connection, () -> connection.getHeaderFieldKey(index));
    }

    public static String replacementForHttpHeaderFieldKey(HttpURLConnection connection, int index) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldKey
        // method.
        return replace(connection, () -> connection.getHeaderFieldKey(index));
    }

    public static String replacementForHeaderField(URLConnection connection, int index) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderField method.
        return replace(connection, () -> connection.getHeaderField(index));
    }

    public static String replacementForHttpHeaderField(HttpURLConnection connection, int index) {
        // URLConnection also overrides this and that is covered in replacementForHeaderField
        // method.
        return replace(connection, () -> connection.getHeaderField(index));
    }

    public static int replacementForResponseCode(URLConnection connection) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        return replaceThrowable(connection, httpURLConnection::getResponseCode);
    }

    public static String replacementForResponseMessage(URLConnection connection)
            throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
        return replaceThrowable(connection, httpURLConnection::getResponseMessage);
    }

    public static OutputStream replacementForOutputStream(URLConnection connection)
            throws IOException {
        return replaceThrowable(connection, connection::getOutputStream, false);
    }

    public static InputStream replacementForInputStream(URLConnection connection)
            throws IOException {
        startTracingAtFirstConnection(connection);

        InputStream inputStream;
        try {
            inputStream = connection.getInputStream();
        } catch (IOException exception) {
            reportWithThrowable(connection, exception);
            throw exception;
        }

        if (inputStream == null) {
            return inputStream;
        }

        return new InstrumentedInputStream(connection, inputStream);
    }

    public static InputStream replacementForErrorStream(HttpURLConnection connection) {
        startTracingAtFirstConnection(connection);

        InputStream errorStream = connection.getErrorStream();

        if (errorStream == null) {
            return errorStream;
        }

        return new InstrumentedInputStream(connection, errorStream);
    }

    private static <T> T replace(URLConnection connection, ResultProvider<T> resultProvider) {
        startTracingAtFirstConnection(connection);

        T result = resultProvider.get();

        updateLastSeenTime(connection);
        markHarvestable(connection);

        return result;
    }

    private static <T> T replaceThrowable(
            URLConnection connection, ThrowableResultProvider<T> resultProvider)
            throws IOException {
        return replaceThrowable(connection, resultProvider, true);
    }

    private static <T> T replaceThrowable(
            URLConnection connection,
            ThrowableResultProvider<T> resultProvider,
            boolean shouldMarkHarvestable)
            throws IOException {
        startTracingAtFirstConnection(connection);

        T result;
        try {
            result = resultProvider.get();
        } catch (IOException exception) {
            reportWithThrowable(connection, exception);
            throw exception;
        }

        updateLastSeenTime(connection);
        if (shouldMarkHarvestable) {
            markHarvestable(connection);
        }

        return result;
    }

    interface ResultProvider<T> {
        T get();
    }

    interface ThrowableResultProvider<T> {
        T get() throws IOException;
    }

    private static void reportWithThrowable(URLConnection connection, IOException exception) {
        endTracing(connection, UNKNOWN_RESPONSE_CODE, exception);
    }

    private static void reportWithResponseCode(HttpURLConnection connection) {
        try {
            endTracing(connection, connection.getResponseCode(), null);
        } catch (IOException exception) {
            logger.log(
                    Level.FINE,
                    "Exception "
                            + exception.getMessage()
                            + " was thrown while ending span for connection "
                            + connection);
        }
    }

    private static void endTracing(URLConnection connection, int responseCode, Throwable error) {
        HttpURLConnectionInfo info = activeURLConnections.get(connection);
        if (info != null && !info.reported) {
            Context context = info.context;
            HttpUrlConnectionSingletons.instrumenter()
                    .end(context, connection, responseCode, error);
            info.reported = true;
            activeURLConnections.remove(connection);
        }
    }

    private static void startTracingAtFirstConnection(URLConnection connection) {
        Context parentContext = Context.current();
        if (!HttpUrlConnectionSingletons.instrumenter().shouldStart(parentContext, connection)) {
            return;
        }

        if (!activeURLConnections.containsKey(connection)) {
            Context context =
                    HttpUrlConnectionSingletons.instrumenter().start(parentContext, connection);
            activeURLConnections.put(connection, new HttpURLConnectionInfo(context));
            try {
                injectContextToRequest(connection, context);
            } catch (Exception exception) {
                // If connection was already made prior to setting this request property,
                // (which should not happen as we've instrumented all methods that connect)
                // above call would throw IllegalStateException.
                logger.log(
                        Level.FINE,
                        "Exception "
                                + exception.getMessage()
                                + " was thrown while adding distributed tracing context for connection "
                                + connection,
                        exception);
            }
        }
    }

    private static void injectContextToRequest(URLConnection connection, Context context) {
        HttpUrlConnectionSingletons.openTelemetryInstance()
                .getPropagators()
                .getTextMapPropagator()
                .inject(context, connection, RequestPropertySetter.INSTANCE);
    }

    private static void updateLastSeenTime(URLConnection connection) {
        final HttpURLConnectionInfo info = activeURLConnections.get(connection);
        if (info != null && !info.reported) {
            info.lastSeenTime = SystemClock.uptimeMillis();
        }
    }

    private static void markHarvestable(URLConnection connection) {
        final HttpURLConnectionInfo info = activeURLConnections.get(connection);
        if (info != null && !info.reported) {
            info.harvestable = true;
        }
    }

    static void reportIdleConnectionsOlderThan(long timeInterval) {
        final long timeNow = SystemClock.uptimeMillis();
        for (URLConnection connection : activeURLConnections.keySet()) {
            final HttpURLConnectionInfo info = activeURLConnections.get(connection);
            if (info != null
                    && info.harvestable
                    && !info.reported
                    && (info.lastSeenTime + timeInterval) < timeNow) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
                reportWithResponseCode(httpURLConnection);
            }
        }
    }

    private static class HttpURLConnectionInfo {
        private final Context context;
        private long lastSeenTime;
        private boolean reported;
        private boolean harvestable;

        private HttpURLConnectionInfo(Context context) {
            this.context = context;
            lastSeenTime = SystemClock.uptimeMillis();
        }
    }

    private static class InstrumentedInputStream extends InputStream {
        private final URLConnection connection;

        private final InputStream inputStream;

        public InstrumentedInputStream(URLConnection connection, InputStream inputStream) {
            this.connection = connection;
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            int res;
            try {
                res = inputStream.read();
            } catch (IOException exception) {
                reportWithThrowable(connection, exception);
                throw exception;
            }
            reportIfDoneOrMarkHarvestable(res);
            return res;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int res;
            try {
                res = inputStream.read(b);
            } catch (IOException exception) {
                reportWithThrowable(connection, exception);
                throw exception;
            }
            reportIfDoneOrMarkHarvestable(res);
            return res;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int res;
            try {
                res = inputStream.read(b, off, len);
            } catch (IOException exception) {
                reportWithThrowable(connection, exception);
                throw exception;
            }
            reportIfDoneOrMarkHarvestable(res);
            return res;
        }

        @Override
        public void close() throws IOException {
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            reportWithResponseCode(httpURLConnection);
            inputStream.close();
        }

        private void reportIfDoneOrMarkHarvestable(int result) {
            if (result == -1) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
                reportWithResponseCode(httpURLConnection);
            } else {
                markHarvestable(connection);
            }
        }
    }
}
