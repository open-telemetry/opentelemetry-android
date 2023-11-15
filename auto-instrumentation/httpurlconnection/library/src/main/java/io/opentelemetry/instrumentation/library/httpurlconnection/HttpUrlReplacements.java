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

    public static synchronized void replacementForConnect(URLConnection c) throws IOException {
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

    public static synchronized Object replacementForContent(URLConnection c) throws IOException {
        startTracingAtFirstConnection(c);

        Object content;
        try {
            content = c.getContent();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        updateLastSeenTime(c);
        markHarvestable(c);

        return content;
    }

    public static synchronized Object replacementForContent(URLConnection c, Class<?>[] classes)
            throws IOException {
        startTracingAtFirstConnection(c);

        Object content;
        try {
            content = c.getContent(classes);
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        updateLastSeenTime(c);
        markHarvestable(c);

        return content;
    }

    public static synchronized String replacementForContentType(URLConnection c) {
        startTracingAtFirstConnection(c);

        String contentType = c.getContentType();

        updateLastSeenTime(c);
        markHarvestable(c);

        return contentType;
    }

    public static synchronized String replacementForContentEncoding(URLConnection c) {
        startTracingAtFirstConnection(c);

        String contentEncoding = c.getContentEncoding();

        updateLastSeenTime(c);
        markHarvestable(c);

        return contentEncoding;
    }

    public static synchronized int replacementForContentLength(URLConnection c) {
        startTracingAtFirstConnection(c);

        int contentLength = c.getContentLength();

        updateLastSeenTime(c);
        markHarvestable(c);

        return contentLength;
    }

    // TODO: uncomment and correct return value when animal sniffer is disabled
    public static synchronized long replacementForContentLengthLong(URLConnection c) {
        startTracingAtFirstConnection(c);

        // long contentLengthLong = c.getContentLengthLong();

        updateLastSeenTime(c);
        markHarvestable(c);

        // return contentLengthLong;
        return 1L;
    }

    public static synchronized long replacementForExpiration(URLConnection c) {
        startTracingAtFirstConnection(c);

        long expiration = c.getExpiration();

        updateLastSeenTime(c);
        markHarvestable(c);

        return expiration;
    }

    public static synchronized long replacementForDate(URLConnection c) {
        startTracingAtFirstConnection(c);

        long date = c.getDate();

        updateLastSeenTime(c);
        markHarvestable(c);

        return date;
    }

    public static synchronized long replacementForLastModified(URLConnection c) {
        startTracingAtFirstConnection(c);

        long lastModified = c.getLastModified();

        updateLastSeenTime(c);
        markHarvestable(c);

        return lastModified;
    }

    public static synchronized String replacementForHeaderField(URLConnection c, String name) {
        startTracingAtFirstConnection(c);

        String headerField = c.getHeaderField(name);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerField;
    }

    public static synchronized Map<String, List<String>> replacementForHeaderFields(
            URLConnection c) {
        startTracingAtFirstConnection(c);

        Map<String, List<String>> headerFields = c.getHeaderFields();

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFields;
    }

    public static synchronized int replacementForHeaderFieldInt(
            URLConnection c, String name, int Default) {
        startTracingAtFirstConnection(c);

        int headerFieldInt = c.getHeaderFieldInt(name, Default);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldInt;
    }

    // TODO: uncomment and correct return value when animal sniffer is disabled
    public static synchronized long replacementForHeaderFieldLong(
            URLConnection c, String name, long Default) {
        startTracingAtFirstConnection(c);

        // long headerFieldLong = c.getHeaderFieldLong(name, Default);

        updateLastSeenTime(c);
        markHarvestable(c);

        // return headerFieldLong;
        return 1L;
    }

    public static synchronized long replacementForHeaderFieldDate(
            URLConnection c, String name, long Default) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldDate method.
        startTracingAtFirstConnection(c);

        long headerFieldDate = c.getHeaderFieldDate(name, Default);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldDate;
    }

    public static synchronized long replacementForHttpHeaderFieldDate(
            HttpURLConnection c, String name, long Default) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldDate
        // method.
        startTracingAtFirstConnection(c);

        long headerFieldDate = c.getHeaderFieldDate(name, Default);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldDate;
    }

    public static synchronized String replacementForHeaderFieldKey(URLConnection c, int n) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldKey method.
        startTracingAtFirstConnection(c);

        String headerFieldKey = c.getHeaderFieldKey(n);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldKey;
    }

    public static synchronized String replacementForHttpHeaderFieldKey(HttpURLConnection c, int n) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldKey
        // method.
        startTracingAtFirstConnection(c);

        String headerFieldKey = c.getHeaderFieldKey(n);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldKey;
    }

    public static synchronized String replacementForHeaderField(URLConnection c, int n) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderField method.
        startTracingAtFirstConnection(c);

        String headerField = c.getHeaderField(n);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerField;
    }

    public static synchronized String replacementForHttpHeaderField(HttpURLConnection c, int n) {
        // URLConnection also overrides this and that is covered in replacementForHeaderField
        // method.
        startTracingAtFirstConnection(c);

        String headerField = c.getHeaderField(n);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerField;
    }

    public static synchronized int replacementForResponseCode(URLConnection c) throws IOException {
        startTracingAtFirstConnection(c);

        int responseCode;
        HttpURLConnection con = (HttpURLConnection) c;
        try {
            responseCode = con.getResponseCode();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        updateLastSeenTime(c);
        markHarvestable(c);

        return responseCode;
    }

    public static synchronized String replacementForResponseMessage(URLConnection c)
            throws IOException {
        startTracingAtFirstConnection(c);

        String responseMessage;
        HttpURLConnection httpURLConnection = (HttpURLConnection) c;
        try {
            responseMessage = httpURLConnection.getResponseMessage();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        updateLastSeenTime(c);
        markHarvestable(c);

        return responseMessage;
    }

    public static synchronized OutputStream replacementForOutputStream(URLConnection c)
            throws IOException {
        startTracingAtFirstConnection(c);

        OutputStream outputStream;
        try {
            outputStream = c.getOutputStream();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        updateLastSeenTime(c);
        // getOutputStream() does not read anything from connection so request not harvestable yet
        // (not reportable if left idle).

        return outputStream;
    }

    public static synchronized InputStream replacementForInputStream(URLConnection c)
            throws IOException {
        startTracingAtFirstConnection(c);

        InputStream inputStream;
        try {
            inputStream = c.getInputStream();
        } catch (IOException e) {
            reportWithThrowable(c, e);
            throw e;
        }

        HttpURLConnection httpURLConnection = (HttpURLConnection) c;
        reportWithResponseCode(httpURLConnection);

        return inputStream;
    }

    public static synchronized InputStream replacementForErrorStream(HttpURLConnection c) {
        startTracingAtFirstConnection(c);

        InputStream errorStream = c.getErrorStream();

        reportWithResponseCode(c);

        return errorStream;
    }

    private static synchronized void reportWithThrowable(URLConnection c, IOException e) {
        endTracing(c, UNKNOWN_RESPONSE_CODE, e);
    }

    private static synchronized void reportWithResponseCode(HttpURLConnection c) {
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

    @SuppressWarnings("MustBeClosedChecker")
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

    private static synchronized void injectContextToRequest(
            URLConnection connection, Context context) {
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
