/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection;

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

    // Time (ms) to wait before assuming that an idle connection is no longer
    // in use and should be reported.
    // TODO: Convert to a configuration
    public static final long CONNECTION_KEEP_ALIVE_INTERVAL = 10000;

    static {
        activeURLConnections = new WeakHashMap<>();
    }

    public static synchronized void replacementForConnect(URLConnection c) throws IOException {
        addTraceContextBeforeFirstConnection(c);

        try {
            c.connect();
        } catch (IOException e) {
            // TODO: Report this request with a http span;
            markReported(c);
            throw e;
        }

        updateLastSeenTime(c);
        // connect() does not read anything from connection so request not harvestable yet.
    }

    public static synchronized Object replacementForContent(URLConnection c) throws IOException {
        addTraceContextBeforeFirstConnection(c);

        Object content;
        try {
            content = c.getContent();
        } catch (IOException e) {
            // TODO: Report this request with a http span;
            markReported(c);
            throw e;
        }

        updateLastSeenTime(c);
        markHarvestable(c);

        return content;
    }

    public static synchronized Object replacementForContent(URLConnection c, Class<?>[] classes)
            throws IOException {
        addTraceContextBeforeFirstConnection(c);

        Object content;
        try {
            content = c.getContent(classes);
        } catch (IOException e) {
            // TODO: Report this request with a http span;
            markReported(c);
            throw e;
        }

        updateLastSeenTime(c);
        markHarvestable(c);

        return content;
    }

    public static synchronized String replacementForContentType(URLConnection c) {
        addTraceContextBeforeFirstConnection(c);

        String contentType = c.getContentType();

        updateLastSeenTime(c);
        markHarvestable(c);

        return contentType;
    }

    public static synchronized String replacementForContentEncoding(URLConnection c) {
        addTraceContextBeforeFirstConnection(c);

        String contentEncoding = c.getContentEncoding();

        updateLastSeenTime(c);
        markHarvestable(c);

        return contentEncoding;
    }

    public static synchronized int replacementForContentLength(URLConnection c) {
        addTraceContextBeforeFirstConnection(c);

        int contentLength = c.getContentLength();

        updateLastSeenTime(c);
        markHarvestable(c);

        return contentLength;
    }

    //TODO: uncomment and correct return value when animal sniffer is disabled
    public static synchronized long replacementForContentLengthLong(URLConnection c) {
        addTraceContextBeforeFirstConnection(c);

        // long contentLengthLong = c.getContentLengthLong();

        updateLastSeenTime(c);
        markHarvestable(c);

        //return contentLengthLong;
        return 1L;
    }

    public static synchronized long replacementForExpiration(URLConnection c) {
        addTraceContextBeforeFirstConnection(c);

        long expiration = c.getExpiration();

        updateLastSeenTime(c);
        markHarvestable(c);

        return expiration;
    }

    public static synchronized long replacementForDate(URLConnection c) {
        addTraceContextBeforeFirstConnection(c);

        long date = c.getDate();

        updateLastSeenTime(c);
        markHarvestable(c);

        return date;
    }

    public static synchronized long replacementForLastModified(URLConnection c) {
        addTraceContextBeforeFirstConnection(c);

        long lastModified = c.getLastModified();

        updateLastSeenTime(c);
        markHarvestable(c);

        return lastModified;
    }

    public static synchronized String replacementForHeaderField(URLConnection c, String name) {
        addTraceContextBeforeFirstConnection(c);

        String headerField = c.getHeaderField(name);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerField;
    }

    public static synchronized Map<String, List<String>> replacementForHeaderFields(
            URLConnection c) {
        addTraceContextBeforeFirstConnection(c);

        Map<String, List<String>> headerFields = c.getHeaderFields();

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFields;
    }

    public static synchronized int replacementForHeaderFieldInt(
            URLConnection c, String name, int Default) {
        addTraceContextBeforeFirstConnection(c);

        int headerFieldInt = c.getHeaderFieldInt(name, Default);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldInt;
    }

    //TODO: uncomment and correct return value when animal sniffer is disabled
    public static synchronized long replacementForHeaderFieldLong(
            URLConnection c, String name, long Default) {
        addTraceContextBeforeFirstConnection(c);

        //long headerFieldLong = c.getHeaderFieldLong(name, Default);

        updateLastSeenTime(c);
        markHarvestable(c);

        //return headerFieldLong;
        return 1L;
    }

    public static synchronized long replacementForHeaderFieldDate(
            URLConnection c, String name, long Default) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldDate method.
        addTraceContextBeforeFirstConnection(c);

        long headerFieldDate = c.getHeaderFieldDate(name, Default);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldDate;
    }

    public static synchronized long replacementForHttpHeaderFieldDate(
            HttpURLConnection c, String name, long Default) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldDate
        // method.
        addTraceContextBeforeFirstConnection(c);

        long headerFieldDate = c.getHeaderFieldDate(name, Default);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldDate;
    }

    public static synchronized String replacementForHeaderFieldKey(URLConnection c, int n) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderFieldKey method.
        addTraceContextBeforeFirstConnection(c);

        String headerFieldKey = c.getHeaderFieldKey(n);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldKey;
    }

    public static synchronized String replacementForHttpHeaderFieldKey(HttpURLConnection c, int n) {
        // URLConnection also overrides this and that is covered in replacementForHeaderFieldKey
        // method.
        addTraceContextBeforeFirstConnection(c);

        String headerFieldKey = c.getHeaderFieldKey(n);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerFieldKey;
    }

    public static synchronized String replacementForHeaderField(URLConnection c, int n) {
        // HttpURLConnection also overrides this and that is covered in
        // replacementForHttpHeaderField method.
        addTraceContextBeforeFirstConnection(c);

        String headerField = c.getHeaderField(n);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerField;
    }

    public static synchronized String replacementForHttpHeaderField(HttpURLConnection c, int n) {
        // URLConnection also overrides this and that is covered in replacementForHeaderField
        // method.
        addTraceContextBeforeFirstConnection(c);

        String headerField = c.getHeaderField(n);

        updateLastSeenTime(c);
        markHarvestable(c);

        return headerField;
    }

    public static synchronized int replacementForResponseCode(URLConnection c) throws IOException {
        addTraceContextBeforeFirstConnection(c);

        int responseCode;
        HttpURLConnection con = (HttpURLConnection) c;
        try {
            responseCode = con.getResponseCode();
        } catch (IOException e) {
            // TODO: Report this request with a http span;
            markReported(c);
            throw e;
        }

        updateLastSeenTime(c);
        markHarvestable(c);

        return responseCode;
    }

    public static synchronized String replacementForResponseMessage(URLConnection c)
            throws IOException {
        addTraceContextBeforeFirstConnection(c);

        String responseMessage;
        HttpURLConnection httpURLConnection = (HttpURLConnection) c;
        try {
            responseMessage = httpURLConnection.getResponseMessage();
        } catch (IOException e) {
            // TODO: Report this request with a http span;
            markReported(c);
            throw e;
        }

        updateLastSeenTime(c);
        markHarvestable(c);

        return responseMessage;
    }

    public static synchronized OutputStream replacementForOutputStream(URLConnection c)
            throws IOException {
        addTraceContextBeforeFirstConnection(c);

        OutputStream outputStream;
        try {
            outputStream = c.getOutputStream();
        } catch (IOException e) {
            // TODO: Report this request with a http span;
            markReported(c);
            throw e;
        }

        updateLastSeenTime(c);
        // getOutputStream() does not read anything from connection so request not harvestable yet.

        return outputStream;
    }

    public static synchronized InputStream replacementForInputStream(URLConnection c)
            throws IOException {
        addTraceContextBeforeFirstConnection(c);

        InputStream inputStream;
        try {
            inputStream = c.getInputStream();
        } catch (IOException e) {
            // TODO: Report this request with a http span;
            markReported(c);
            throw e;
        }

        // TODO: Report this request with a http span;
        markReported(c);

        return inputStream;
    }

    public static synchronized InputStream replacementForErrorStream(HttpURLConnection c) {
        addTraceContextBeforeFirstConnection(c);

        InputStream errorStream = c.getErrorStream();

        // TODO: Report this request with a http span;
        markReported(c);

        return errorStream;
    }

    private static synchronized void addTraceContextBeforeFirstConnection(URLConnection c) {
        if (activeURLConnections.get(c) == null) {
            activeURLConnections.put(c, new HttpURLConnectionInfo());
            try {
                // TODO: Start span and add trace context using
                // c.setRequestProperty("propertyKey","propertyValue");
            } catch (Exception e) {
                // If connection was already made prior to setting this request property,
                // (which should not happen as we've instrumented all methods that connect)
                // above call would throw IllegalStateException. Capture and report this
                // as we need to know if tracing was not set for any request.
                // TODO: Report this request with a http span;
                markReported(c);
            }
        }
    }

    private static synchronized void updateLastSeenTime(URLConnection c) {
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null) {
            info.lastSeenTime = System.currentTimeMillis();
        }
    }

    private static synchronized void markHarvestable(URLConnection c) {
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null) {
            info.harvestable = true;
        }
    }

    private static synchronized void markReported(URLConnection c) {
        final HttpURLConnectionInfo info = activeURLConnections.get(c);
        if (info != null) {
            info.reported = true;
        }
    }

    // TODO: Move scheduleConnectionKiller in a different class as it's the only client callable API
    // in this class
    public Runnable scheduleConnectionKiller() {
        return new Runnable() {
            @Override
            public void run() {
                endOngoingConnectionsOlderThan(CONNECTION_KEEP_ALIVE_INTERVAL);
            }

            @Override
            public String toString() {
                return "EndOngoingConnectionsRunnable";
            }
        };
    }

    synchronized void endOngoingConnectionsOlderThan(long time) {
        final long timeNow = System.currentTimeMillis();
        for (URLConnection c : activeURLConnections.keySet()) {
            final HttpURLConnectionInfo info = activeURLConnections.get(c);

            if (info != null
                    && info.harvestable
                    && !info.reported
                    && (info.lastSeenTime + time) < timeNow) {
                // TODO: Report this request with a http span;
                markReported(c);
            }
        }
    }

    private static class HttpURLConnectionInfo {
        private long lastSeenTime;
        private boolean reported;
        private boolean harvestable;

        private HttpURLConnectionInfo() {
            lastSeenTime = System.currentTimeMillis();
        }
    }
}
