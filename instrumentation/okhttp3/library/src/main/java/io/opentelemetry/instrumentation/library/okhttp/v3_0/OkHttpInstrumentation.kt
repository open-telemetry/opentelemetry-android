/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("ktlint:standard:package-name")

package io.opentelemetry.instrumentation.library.okhttp.v3_0

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceResolver
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.internal.HttpConstants
import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttp3Singletons
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Instrumentation for okhttp requests.
 */
@AutoService(AndroidInstrumentation::class)
class OkHttpInstrumentation : AndroidInstrumentation {
    @JvmField
    val additionalExtractors: MutableList<AttributesExtractor<Interceptor.Chain, Response>> =
        mutableListOf()

    /**
     * Configures the HTTP request headers that will be captured as span attributes as described in
     * [HTTP semantic conventions](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/http.md#http-request-and-response-headers).
     *
     * The HTTP request header values will be captured under the `http.request.header.<name>`
     * attribute key. The `<name>` part in the attribute key is
     * the normalized header name: lowercase, with dashes replaced by underscores.
     *
     * @param capturedRequestHeaders A list of HTTP header names.
     */
    var capturedRequestHeaders: MutableList<String> = mutableListOf()
        set(requestHeaders) {
            field = requestHeaders.toMutableList()
        }

    /**
     * Configures the HTTP response headers that will be captured as span attributes as described in
     * [HTTP semantic conventions](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/http.md#http-request-and-response-headers).
     *
     * The HTTP response header values will be captured under the `http.response.header.<name>`
     * attribute key. The `<name>` part in the attribute key is
     * the normalized header name: lowercase, with dashes replaced by underscores.
     *
     * @param capturedResponseHeaders A list of HTTP header names.
     */
    var capturedResponseHeaders: MutableList<String> = mutableListOf()
        set(responseHeaders) {
            field = responseHeaders.toMutableList()
        }

    /**
     * Configures the attrs extractor to recognize an alternative set of HTTP request methods.
     *
     * By default, the extractor defines "known" methods as the ones listed in
     * [RFC9110](https://www.rfc-editor.org/rfc/rfc9110.html#name-methods) and the PATCH
     * method defined in [RFC5789](https://www.rfc-editor.org/rfc/rfc5789.html). If an
     * unknown method is encountered, the extractor will use the value {@value HttpConstants#_OTHER}
     * instead of it and put the original value in an extra `http.request.method_original`
     * attribute.
     *
     * Note: calling this method **overrides** the default known method sets completely; it
     * does not supplement it.
     *
     * @param knownMethods A set of recognized HTTP request methods.
     */
    var knownMethods: MutableSet<String> = HttpConstants.KNOWN_METHODS
        set(knownMethods) {
            field = knownMethods.toMutableSet()
        }

    private var peerServiceMapping: Map<String, String> = mapOf()
    private var emitExperimentalHttpClientTelemetry = false

    /**
     * Adds an [AttributesExtractor] that will extract additional attributes.
     */
    fun addAttributesExtractor(extractor: AttributesExtractor<Interceptor.Chain, Response>) {
        additionalExtractors.add(extractor)
    }

    /**
     * Configures the extractor of the `peer.service` span attribute, described in
     * [the specification](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/span-general.md#general-remote-service-attributes).
     */
    fun setPeerServiceMapping(peerServiceMapping: MutableMap<String, String>) {
        this.peerServiceMapping = peerServiceMapping.toMap()
    }

    fun newPeerServiceResolver(): PeerServiceResolver = PeerServiceResolver.create(peerServiceMapping)

    /**
     * When enabled keeps track of [non-stable
     * HTTP client metrics](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/http/http-metrics.md#http-client): [the
     * request size ](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/http/http-metrics.md#metric-httpclientrequestbodysize) and the [
     * the response size](https://github.com/open-telemetry/semantic-conventions/blob/main/docs/http/http-metrics.md#metric-httpserverresponsebodysize).
     */
    fun setEmitExperimentalHttpClientTelemetry(emitExperimentalHttpClientTelemetry: Boolean) {
        this.emitExperimentalHttpClientTelemetry = emitExperimentalHttpClientTelemetry
    }

    fun emitExperimentalHttpClientTelemetry(): Boolean = emitExperimentalHttpClientTelemetry

    override fun install(ctx: InstallationContext) {
        OkHttp3Singletons.configure(this, ctx.openTelemetry)
    }

    override val name: String = "okhttp"
}
