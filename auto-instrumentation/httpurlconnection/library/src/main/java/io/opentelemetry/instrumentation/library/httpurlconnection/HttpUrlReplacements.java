/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection;

import static io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlConnectionSingletons.instrumenter;

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
import java.util.WeakHashMap;

public class HttpUrlReplacements {

    // WeakHashMap has weak references to the key, and when the key is garbage collected
    // the entry is effectively removed from the map. This means that we never have to
    // remove entries from the map.
    private static final WeakHashMap<URLConnection, HttpURLConnectionInfo> activeURLConnections;
    public static final int UNKNOWN_RESPONSE_CODE = -1;

    static {
        activeURLConnections = new WeakHashMap<>();
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
        return replaceThrowable(c, c::getOutputStream, false, false);
    }

    public static InputStream replacementForInputStream(URLConnection c) throws IOException {
        return replaceThrowable(c, c::getInputStream, false, true);
    }

    public static InputStream replacementForErrorStream(HttpURLConnection c) {
        startTracingAtFirstConnection(c);

        InputStream errorStream = c.getErrorStream();

        reportWithResponseCode(c);

        return errorStream;
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
        return replaceThrowable(c, resultProvider, true, false);
    }

    private static <T> T replaceThrowable(
            URLConnection c,
            ThrowableResultProvider<T> resultProvider,
            boolean shouldMarkHarvestable,
            boolean reportWithResponseCode)
            throws IOException {
        startTracingAtFirstConnection(c);

        T result;
        try {
            result = resultProvider.get();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        if (reportWithResponseCode) {
            HttpURLConnection httpURLConnection = (HttpURLConnection) c;
            reportWithResponseCode(httpURLConnection);
        } else {
            updateLastSeenTime(c);
            if (shouldMarkHarvestable) {
                markHarvestable(c);
            }
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
            // TODO: Log instrumentation error in getting response code
        }
    }

    private static synchronized void endTracing(
            URLConnection c, int responseCode, Throwable error) {
        HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            Context context = info.context;
            instrumenter().end(context, c, responseCode, error);
            info.reported = true;
        }
    }

    private static synchronized void startTracingAtFirstConnection(URLConnection c) {
        Context parentContext = Context.current();
        if (!instrumenter().shouldStart(parentContext, c)) {
            return;
        }

        if (activeURLConnections.get(c) == null) {
            Context context = instrumenter().start(parentContext, c);
            activeURLConnections.put(c, new HttpURLConnectionInfo(context));
            try {
                injectContextToRequest(c, context);
            } catch (Exception e) {
                // If connection was already made prior to setting this request property,
                // (which should not happen as we've instrumented all methods that connect)
                // above call would throw IllegalStateException.
                // TODO: Log instrumentation error in trying to add request header for tracing
            }
        }
    }

    private static void injectContextToRequest(URLConnection connection, Context context) {
        GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(context, connection, RequestPropertySetter.INSTANCE);
    }

    private static synchronized void updateLastSeenTime(URLConnection c) {
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            info.lastSeenTime = System.nanoTime();
        }
    }

    private static synchronized void markHarvestable(URLConnection c) {
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null && !info.reported) {
            info.harvestable = true;
        }
    }

    static synchronized void reportIdleConnectionsOlderThan(long timeIntervalInNanoSec) {
        final long timeNow = System.nanoTime();
        for (URLConnection c : activeURLConnections.keySet()) {
            final HttpURLConnectionInfo info = activeURLConnections.get(c);
            if (info != null
                    && info.harvestable
                    && !info.reported
                    && (info.lastSeenTime + timeIntervalInNanoSec) < timeNow) {
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
            // Using System.nanoTime() as it is independent of device clock and any changes to that.
            lastSeenTime = System.nanoTime();
        }
    }
}
