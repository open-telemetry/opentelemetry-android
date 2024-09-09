/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection;

import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceResolver;
import io.opentelemetry.instrumentation.api.internal.HttpConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Configuration for automatic instrumentation of HttpURLConnection/HttpsURLConnection requests. */
public final class HttpUrlInstrumentationConfig {
    private static List<String> capturedRequestHeaders = new ArrayList<>();
    private static List<String> capturedResponseHeaders = new ArrayList<>();
    private static Set<String> knownMethods = HttpConstants.KNOWN_METHODS;
    private static Map<String, String> peerServiceMapping = new HashMap<>();
    private static boolean emitExperimentalHttpClientMetrics;

    // Time (ms) to wait before assuming that an idle connection is no longer
    // in use and should be reported.
    private static long connectionInactivityTimeoutMs = 10000;

    private HttpUrlInstrumentationConfig() {}

    /**
     * Configures the HTTP request headers that will be captured as span attributes as described in
     * <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/4f23dce407b6fcaba34a049df7c3d41cdd58cb77/specification/trace/semantic_conventions/http.md#http-request-and-response-headers">HTTP
     * semantic conventions</a>.
     *
     * <p>The HTTP request header values will be captured under the {@code
     * http.request.header.<name>} attribute key. The {@code <name>} part in the attribute key is
     * the normalized header name: lowercase, with dashes replaced by underscores.
     *
     * @param requestHeaders A list of HTTP header names.
     */
    public static void setCapturedRequestHeaders(List<String> requestHeaders) {
        HttpUrlInstrumentationConfig.capturedRequestHeaders = new ArrayList<>(requestHeaders);
    }

    public static List<String> getCapturedRequestHeaders() {
        return capturedRequestHeaders;
    }

    /**
     * Configures the HTTP response headers that will be captured as span attributes as described in
     * <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/4f23dce407b6fcaba34a049df7c3d41cdd58cb77/specification/trace/semantic_conventions/http.md#http-request-and-response-headers">HTTP
     * semantic conventions</a>.
     *
     * <p>The HTTP response header values will be captured under the {@code
     * http.response.header.<name>} attribute key. The {@code <name>} part in the attribute key is
     * the normalized header name: lowercase, with dashes replaced by underscores.
     *
     * @param responseHeaders A list of HTTP header names.
     */
    public static void setCapturedResponseHeaders(List<String> responseHeaders) {
        HttpUrlInstrumentationConfig.capturedResponseHeaders = new ArrayList<>(responseHeaders);
    }

    public static List<String> getCapturedResponseHeaders() {
        return capturedResponseHeaders;
    }

    /**
     * Configures the attrs extractor to recognize an alternative set of HTTP request methods.
     *
     * <p>By default, the extractor defines "known" methods as the ones listed in <a
     * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-methods">RFC9110</a> and the PATCH
     * method defined in <a href="https://www.rfc-editor.org/rfc/rfc5789.html">RFC5789</a>. If an
     * unknown method is encountered, the extractor will use the value {@value HttpConstants#_OTHER}
     * instead of it and put the original value in an extra {@code http.request.method_original}
     * attribute.
     *
     * <p>Note: calling this method <b>overrides</b> the default known method sets completely; it
     * does not supplement it.
     *
     * @param knownMethods A set of recognized HTTP request methods.
     */
    public static void setKnownMethods(Set<String> knownMethods) {
        HttpUrlInstrumentationConfig.knownMethods = new HashSet<>(knownMethods);
    }

    public static Set<String> getKnownMethods() {
        return knownMethods;
    }

    /**
     * Configures the extractor of the {@code peer.service} span attribute, described in <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/4f23dce407b6fcaba34a049df7c3d41cdd58cb77/specification/trace/semantic_conventions/span-general.md#general-remote-service-attributes">the
     * specification</a>.
     */
    public static void setPeerServiceMapping(Map<String, String> peerServiceMapping) {
        HttpUrlInstrumentationConfig.peerServiceMapping = new HashMap<>(peerServiceMapping);
    }

    public static PeerServiceResolver newPeerServiceResolver() {
        return PeerServiceResolver.create(peerServiceMapping);
    }

    /**
     * When enabled keeps track of <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/4f23dce407b6fcaba34a049df7c3d41cdd58cb77/specification/metrics/semantic_conventions/http-metrics.md#http-client">the
     * experimental HTTP client metrics</a>.
     */
    public static void setEmitExperimentalHttpClientMetrics(
            boolean emitExperimentalHttpClientMetrics) {
        HttpUrlInstrumentationConfig.emitExperimentalHttpClientMetrics =
                emitExperimentalHttpClientMetrics;
    }

    public static boolean emitExperimentalHttpClientMetrics() {
        return emitExperimentalHttpClientMetrics;
    }

    /**
     * Configures the connection inactivity timeout in milliseconds.
     *
     * <p>This timeout defines the time that should have elapsed since the connection was last
     * active. It is used by the idle connection harvester thread to find idle connections that
     * should be reported. To schedule the idle connection harvester follow instructions defined in
     * the <a
     * href="https://github.com/open-telemetry/opentelemetry-android/blob/96ea4aa9fe709838a91811deeb85b0f1baceb8cd/instrumentation/httpurlconnection/README.md#scheduling-harvester-thread">
     * README</a>. If the specified timeout is negative, an {@code IllegalArgumentException} will be
     * thrown.
     *
     * <p>Default value: "10000"
     *
     * @param timeoutMs the timeout period in milliseconds. Must be non-negative.
     * @throws IllegalArgumentException if {@code timeoutMs} is negative.
     */
    public static void setConnectionInactivityTimeoutMs(long timeoutMs) {
        if (timeoutMs < 0) {
            throw new IllegalArgumentException("timeoutMs must be non-negative");
        }
        HttpUrlInstrumentationConfig.connectionInactivityTimeoutMs = timeoutMs;
    }

    /**
     * Configures the connection inactivity timeout in milliseconds for testing purposes only.
     *
     * <p>This test only API allows you to set negative values for timeout. For production
     * workflows, {@code setConnectionInactivityTimeoutMs} API should be used.
     *
     * @param timeoutMsForTesting the timeout period in milliseconds.
     */
    public static void setConnectionInactivityTimeoutMsForTesting(long timeoutMsForTesting) {
        HttpUrlInstrumentationConfig.connectionInactivityTimeoutMs = timeoutMsForTesting;
    }

    /**
     * Returns a runnable that can be scheduled to run periodically at a fixed interval to close
     * open spans if connection is left idle for connectionInactivityTimeoutMs duration.
     * connectionInactivityTimeoutMs can be obtained via getReportIdleConnectionInterval() API.
     *
     * @return The idle connection reporting runnable
     */
    public static Runnable getReportIdleConnectionRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                HttpUrlReplacements.reportIdleConnectionsOlderThan(connectionInactivityTimeoutMs);
            }

            @Override
            public String toString() {
                return "ReportIdleConnectionsRunnable";
            }
        };
    }

    /**
     * The interval duration in milli seconds that the runnable from
     * getReportIdleConnectionRunnable() API should be scheduled to periodically run at.
     *
     * @return The interval duration in ms
     */
    public static long getReportIdleConnectionInterval() {
        return connectionInactivityTimeoutMs;
    }
}
