/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceResolver;
import io.opentelemetry.instrumentation.api.internal.HttpConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Configuration for automatic instrumentation of okhttp requests. */
public final class OkHttpInstrumentationConfig {
    private static List<String> capturedRequestHeaders = new ArrayList<>();
    private static List<String> capturedResponseHeaders = new ArrayList<>();
    private static Set<String> knownMethods = HttpConstants.KNOWN_METHODS;
    private static Map<String, String> peerServiceMapping = new HashMap<>();
    private static boolean emitExperimentalHttpClientMetrics;

    private OkHttpInstrumentationConfig() {}

    /**
     * Configures the HTTP request headers that will be captured as span attributes as described in
     * <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/http.md#http-request-and-response-headers">HTTP
     * semantic conventions</a>.
     *
     * <p>The HTTP request header values will be captured under the {@code
     * http.request.header.<name>} attribute key. The {@code <name>} part in the attribute key is
     * the normalized header name: lowercase, with dashes replaced by underscores.
     *
     * @param requestHeaders A list of HTTP header names.
     */
    public static void setCapturedRequestHeaders(List<String> requestHeaders) {
        OkHttpInstrumentationConfig.capturedRequestHeaders = new ArrayList<>(requestHeaders);
    }

    public static List<String> getCapturedRequestHeaders() {
        return capturedRequestHeaders;
    }

    /**
     * Configures the HTTP response headers that will be captured as span attributes as described in
     * <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/http.md#http-request-and-response-headers">HTTP
     * semantic conventions</a>.
     *
     * <p>The HTTP response header values will be captured under the {@code
     * http.response.header.<name>} attribute key. The {@code <name>} part in the attribute key is
     * the normalized header name: lowercase, with dashes replaced by underscores.
     *
     * @param responseHeaders A list of HTTP header names.
     */
    public static void setCapturedResponseHeaders(List<String> responseHeaders) {
        OkHttpInstrumentationConfig.capturedResponseHeaders = new ArrayList<>(responseHeaders);
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
        OkHttpInstrumentationConfig.knownMethods = new HashSet<>(knownMethods);
    }

    public static Set<String> getKnownMethods() {
        return knownMethods;
    }

    /**
     * Configures the extractor of the {@code peer.service} span attribute, described in <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/span-general.md#general-remote-service-attributes">the
     * specification</a>.
     */
    public static void setPeerServiceMapping(Map<String, String> peerServiceMapping) {
        OkHttpInstrumentationConfig.peerServiceMapping = new HashMap<>(peerServiceMapping);
    }

    public static PeerServiceResolver newPeerServiceResolver() {
        return PeerServiceResolver.create(peerServiceMapping);
    }

    /**
     * When enabled keeps track of <a
     * href="https://github.com/open-telemetry/semantic-conventions/blob/main/specification/metrics/semantic_conventions/http-metrics.md#http-client">non-stable
     * HTTP client metrics</a>: <a
     * href="https://github.com/open-telemetry/semantic-conventions/blob/main/specification/metrics/semantic_conventions/http-metrics.md#metric-httpclientrequestsize">the
     * request size </a> and the <a
     * href="https://github.com/open-telemetry/semantic-conventions/blob/main/specification/metrics/semantic_conventions/http-metrics.md#metric-httpclientresponsesize">
     * the response size</a>.
     */
    public static void setEmitExperimentalHttpClientMetrics(
            boolean emitExperimentalHttpClientMetrics) {
        OkHttpInstrumentationConfig.emitExperimentalHttpClientMetrics =
                emitExperimentalHttpClientMetrics;
    }

    public static boolean emitExperimentalHttpClientMetrics() {
        return emitExperimentalHttpClientMetrics;
    }
}
