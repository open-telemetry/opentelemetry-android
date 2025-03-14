/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0;

import com.google.auto.service.AutoService;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceResolver;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.HttpConstants;
import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttp3Singletons;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/** Instrumentation for okhttp requests. */
@AutoService(AndroidInstrumentation.class)
public class OkHttpInstrumentation implements AndroidInstrumentation {
    private final List<AttributesExtractor<Interceptor.Chain, Response>> additionalExtractors =
            new ArrayList<>();
    private List<String> capturedRequestHeaders = new ArrayList<>();
    private List<String> capturedResponseHeaders = new ArrayList<>();
    private Set<String> knownMethods = HttpConstants.KNOWN_METHODS;
    private Map<String, String> peerServiceMapping = new HashMap<>();
    private boolean emitExperimentalHttpClientMetrics;

    /** Adds an {@link AttributesExtractor} that will extract additional attributes. */
    public void addAttributesExtractor(AttributesExtractor<Interceptor.Chain, Response> extractor) {
        additionalExtractors.add(extractor);
    }

    public List<AttributesExtractor<Interceptor.Chain, Response>> getAdditionalExtractors() {
        return additionalExtractors;
    }

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
    public void setCapturedRequestHeaders(List<String> requestHeaders) {
        capturedRequestHeaders = new ArrayList<>(requestHeaders);
    }

    public List<String> getCapturedRequestHeaders() {
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
    public void setCapturedResponseHeaders(List<String> responseHeaders) {
        capturedResponseHeaders = new ArrayList<>(responseHeaders);
    }

    public List<String> getCapturedResponseHeaders() {
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
    public void setKnownMethods(Set<String> knownMethods) {
        this.knownMethods = new HashSet<>(knownMethods);
    }

    public Set<String> getKnownMethods() {
        return knownMethods;
    }

    /**
     * Configures the extractor of the {@code peer.service} span attribute, described in <a
     * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/span-general.md#general-remote-service-attributes">the
     * specification</a>.
     */
    public void setPeerServiceMapping(Map<String, String> peerServiceMapping) {
        this.peerServiceMapping = new HashMap<>(peerServiceMapping);
    }

    public PeerServiceResolver newPeerServiceResolver() {
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
    public void setEmitExperimentalHttpClientMetrics(boolean emitExperimentalHttpClientMetrics) {
        this.emitExperimentalHttpClientMetrics = emitExperimentalHttpClientMetrics;
    }

    public boolean emitExperimentalHttpClientMetrics() {
        return emitExperimentalHttpClientMetrics;
    }

    @Override
    public void install(@NotNull InstallationContext ctx) {
        OkHttp3Singletons.configure(this, ctx.getOpenTelemetry());
    }
}
