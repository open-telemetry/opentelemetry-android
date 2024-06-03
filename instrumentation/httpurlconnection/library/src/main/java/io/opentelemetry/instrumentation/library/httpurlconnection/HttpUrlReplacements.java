/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection;

import static io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlConnectionSingletons.instrumenter;

import android.os.SystemClock;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
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

    private static final Map<URLConnection, HttpURLConnectionInfo> activeURLConnections;
    private static final Logger logger;
    public static final int UNKNOWN_RESPONSE_CODE = -1;

    static {
        activeURLConnections = new ConcurrentHashMap<>();
        logger = Logger.getLogger("HttpUrlReplacements");
    }

    public static void replacementForDisconnect(HttpURLConnection c) {
        // Ensure ending of un-ended spans while connection is still alive
        // If disconnect is not called, harvester thread if scheduled, takes care of ending any
        // un-ended spans.
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            reportWithResponseCode(c);
        }

        c.disconnect();
    }

    public static void replacementForConnect(URLConnection c) throws IOException {
        startTracingAtFirstConnection(c);

        try {
            c.connect();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        updateLastSeenTime(c);
        // connect() does not read anything from connection so request not harvestable yet (to be
        // reported if left idle).
    }

    public static Object replacementForContent(URLConnection c) throws IOException {
        return replaceThrowable(c, c::getContent);
    }

    public static Object replacementForContent(URLConnection c, Class<?>[] classes)
            throws IOException {
        return replaceThrowable(c, () -> c.getContent(classes));
    }

    public static String replacementForContentType(URLConnection c) {
        return replace(c, () -> c.getContentType());
    }

    public static String replacementForContentEncoding(URLConnection c) {
        return replace(c, () -> c.getContentEncoding());
    }

    public static int replacementForContentLength(URLConnection c) {
        return replace(c, () -> c.getContentLength());
    }

    public static long replacementForContentLengthLong(URLConnection c) {
        return replace(c, () -> c.getContentLengthLong());
    }

    public static long replacementForExpiration(URLConnection c) {
        return replace(c, () -> c.getExpiration());
    }

    public static long replacementForDate(URLConnection c) {
        return replace(c, () -> c.getDate());
    }

    public static long replacementForLastModified(URLConnection c) {
        return replace(c, () -> c.getLastModified());
    }

    public static String replacementForHeaderField(URLConnection c, String name) {
        return replace(c, () -> c.getHeaderField(name));
    }

    public static Map<String, List<String>> replacementForHeaderFields(URLConnection c) {
        return replace(c, () -> c.getHeaderFields());
    }

    public static int replacementForHeaderFieldInt(URLConnection c, String name, int Default) {
        return replace(c, () -> c.getHeaderFieldInt(name, Default));
    }

    public static long replacementForHeaderFieldLong(URLConnection c, String name, long Default) {
        return replace(c, () -> c.getHeaderFieldLong(name, Default));
    }

    public static long replacementForHeaderFieldDate(URLConnection c, String name, long Default) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldDate method.
        return replace(c, () -> c.getHeaderFieldDate(name, Default));
    }

    public static long replacementForHttpHeaderFieldDate(
            HttpURLConnection c, String name, long Default) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldDate
        // method.
        return replace(c, () -> c.getHeaderFieldDate(name, Default));
    }

    public static String replacementForHeaderFieldKey(URLConnection c, int n) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldKey method.
        return replace(c, () -> c.getHeaderFieldKey(n));
    }

    public static String replacementForHttpHeaderFieldKey(HttpURLConnection c, int n) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldKey
        // method.
        return replace(c, () -> c.getHeaderFieldKey(n));
    }

    public static String replacementForHeaderField(URLConnection c, int n) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderField method.
        return replace(c, () -> c.getHeaderField(n));
    }

    public static String replacementForHttpHeaderField(HttpURLConnection c, int n) {
        // URLConnection also overrides this and that is covered in replacementForHeaderField
        // method.
        return replace(c, () -> c.getHeaderField(n));
    }

    public static int replacementForResponseCode(URLConnection c) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) c;
        return replaceThrowable(c, httpURLConnection::getResponseCode);
    }

    public static String replacementForResponseMessage(URLConnection c) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) c;
        return replaceThrowable(c, httpURLConnection::getResponseMessage);
    }

    public static OutputStream replacementForOutputStream(URLConnection c) throws IOException {
        return replaceThrowable(c, c::getOutputStream, false);
    }

    public static InputStream replacementForInputStream(URLConnection c) throws IOException {
        startTracingAtFirstConnection(c);

        InputStream inputStream;
        try {
            inputStream = c.getInputStream();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        if (inputStream == null) {
            return inputStream;
        }

        return getInstrumentedInputStream(c, inputStream);
    }

    public static InputStream replacementForErrorStream(HttpURLConnection c) {
        startTracingAtFirstConnection(c);

        InputStream errorStream = c.getErrorStream();

        if (errorStream == null) {
            return errorStream;
        }

        return getInstrumentedInputStream(c, errorStream);
    }

    private static InputStream getInstrumentedInputStream(
            URLConnection c, InputStream inputStream) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                int res;
                try {
                    res = inputStream.read();
                } catch (IOException e) {
                    reportWithThrowable(c, e);
                    throw e;
                }
                reportIfDoneOrMarkHarvestable(res);
                return res;
            }

            @Override
            public int read(byte[] b) throws IOException {
                int res;
                try {
                    res = inputStream.read(b);
                } catch (IOException e) {
                    reportWithThrowable(c, e);
                    throw e;
                }
                reportIfDoneOrMarkHarvestable(res);
                return res;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int res;
                try {
                    res = inputStream.read(b, off, len);
                } catch (IOException e) {
                    reportWithThrowable(c, e);
                    throw e;
                }
                reportIfDoneOrMarkHarvestable(res);
                return res;
            }

            @Override
            public void close() throws IOException {
                HttpURLConnection httpURLConnection = (HttpURLConnection) c;
                reportWithResponseCode(httpURLConnection);
                inputStream.close();
            }

            private void reportIfDoneOrMarkHarvestable(int result) {
                if (result == -1) {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) c;
                    reportWithResponseCode(httpURLConnection);
                } else {
                    markHarvestable(c);
                }
            }
        };
    }

    private static <T> T replace(URLConnection c, ResultProvider<T> resultProvider) {
        startTracingAtFirstConnection(c);

        T result = resultProvider.get();

        updateLastSeenTime(c);
        markHarvestable(c);

        return result;
    }

    private static <T> T replaceThrowable(
            URLConnection c, ThrowableResultProvider<T> resultProvider) throws IOException {
        return replaceThrowable(c, resultProvider, true);
    }

    private static <T> T replaceThrowable(
            URLConnection c,
            ThrowableResultProvider<T> resultProvider,
            boolean shouldMarkHarvestable)
            throws IOException {
        startTracingAtFirstConnection(c);

        T result;
        try {
            result = resultProvider.get();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        updateLastSeenTime(c);
        if (shouldMarkHarvestable) {
            markHarvestable(c);
        }

        return result;
    }

    interface ResultProvider<T> {
        T get();
    }

    interface ThrowableResultProvider<T> {
        T get() throws IOException;
    }

    private static void reportWithThrowable(URLConnection c, IOException e) {
        endTracing(c, UNKNOWN_RESPONSE_CODE, e);
    }

    private static void reportWithResponseCode(HttpURLConnection c) {
        try {
            endTracing(c, c.getResponseCode(), null);
        } catch (IOException e) {
            logger.log(
                    Level.FINE,
                    "Exception "
                            + e.getMessage()
                            + " was thrown while ending span for connection "
                            + c.toString());
        }
    }

    private static void endTracing(URLConnection c, int responseCode, Throwable error) {
        HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            Context context = info.context;
            instrumenter().end(context, c, responseCode, error);
            info.reported = true;
            activeURLConnections.remove(c);
        }
    }

    private static void startTracingAtFirstConnection(URLConnection c) {
        Context parentContext = Context.current();
        if (!instrumenter().shouldStart(parentContext, c)) {
            return;
        }

        if (!activeURLConnections.containsKey(c)) {
            Context context = instrumenter().start(parentContext, c);
            activeURLConnections.put(c, new HttpURLConnectionInfo(context));
            try {
                injectContextToRequest(c, context);
            } catch (Exception e) {
                // If connection was already made prior to setting this request property,
                // (which should not happen as we've instrumented all methods that connect)
                // above call would throw IllegalStateException.
                logger.log(
                        Level.FINE,
                        "Exception "
                                + e.getMessage()
                                + " was thrown while adding distributed tracing context for connection "
                                + c,
                        e);
            }
        }
    }

    private static void injectContextToRequest(URLConnection connection, Context context) {
        GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(context, connection, RequestPropertySetter.INSTANCE);
    }

    private static void updateLastSeenTime(URLConnection c) {
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            info.lastSeenTime = SystemClock.uptimeMillis();
        }
    }

    private static void markHarvestable(URLConnection c) {
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            info.harvestable = true;
        }
    }

    static void reportIdleConnectionsOlderThan(long timeInterval) {
        final long timeNow = SystemClock.uptimeMillis();
        for (URLConnection c : activeURLConnections.keySet()) {
            final HttpURLConnectionInfo info = activeURLConnections.get(c);
            if (info != null
                    && info.harvestable
                    && !info.reported
                    && (info.lastSeenTime + timeInterval) < timeNow) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) c;
                reportWithResponseCode(httpURLConnection);
            }
        }
    }

    private static class HttpURLConnectionInfo {
        private long lastSeenTime;
        private boolean reported;
        private boolean harvestable;
        private Context context;

        private HttpURLConnectionInfo(Context context) {
            this.context = context;
            lastSeenTime = SystemClock.uptimeMillis();
        }
    }
}
