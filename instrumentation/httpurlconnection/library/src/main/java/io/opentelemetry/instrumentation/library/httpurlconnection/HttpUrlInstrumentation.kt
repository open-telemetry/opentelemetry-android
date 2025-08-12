/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.httpurlconnection

import com.google.auto.service.AutoService
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceResolver
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.internal.HttpConstants
import io.opentelemetry.instrumentation.library.httpurlconnection.internal.HttpUrlConnectionSingletons
import java.net.URLConnection

/** Instrumentation for HttpURLConnection requests.  */
@AutoService(AndroidInstrumentation::class)
class HttpUrlInstrumentation : AndroidInstrumentation {
    private val additionalExtractorList: MutableList<AttributesExtractor<URLConnection, Int>> = mutableListOf()

    /**
     * Configures the HTTP request headers that will be captured as span attributes as described in
     * [HTTP semantic conventions](https://github.com/open-telemetry/opentelemetry-specification/blob/4f23dce407b6fcaba34a049df7c3d41cdd58cb77/specification/trace/semantic_conventions/http.md#http-request-and-response-headers).
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
     * [HTTP semantic conventions](https://github.com/open-telemetry/opentelemetry-specification/blob/4f23dce407b6fcaba34a049df7c3d41cdd58cb77/specification/trace/semantic_conventions/http.md#http-request-and-response-headers).
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

    private var peerServiceMapping: MutableMap<String, String> = mutableMapOf()
    private var emitExperimentalHttpClientMetrics = false

    /**
     * The interval duration in milli seconds that the runnable from
     * getReportIdleConnectionRunnable() API should be scheduled to periodically run at.
     *
     * @return The interval duration in ms
     */
    var reportIdleConnectionInterval: Long = 10000
        private set

    /** Adds an [AttributesExtractor] that will extract additional attributes.  */
    fun addAttributesExtractor(extractor: AttributesExtractor<URLConnection, Int>) {
        additionalExtractorList.add(extractor)
    }

    fun getAdditionalExtractors(): MutableList<AttributesExtractor<URLConnection, Int>> = additionalExtractorList

    /**
     * Configures the extractor of the `peer.service` span attribute, described in
     * [the specification](https://github.com/open-telemetry/opentelemetry-specification/blob/4f23dce407b6fcaba34a049df7c3d41cdd58cb77/specification/trace/semantic_conventions/span-general.md#general-remote-service-attributes).
     */
    fun setPeerServiceMapping(peerServiceMapping: MutableMap<String, String>) {
        this.peerServiceMapping = peerServiceMapping.toMutableMap()
    }

    fun newPeerServiceResolver(): PeerServiceResolver = PeerServiceResolver.create(peerServiceMapping)

    /**
     * When enabled keeps track of
     * [the experimental HTTP client metrics](https://github.com/open-telemetry/opentelemetry-specification/blob/4f23dce407b6fcaba34a049df7c3d41cdd58cb77/specification/metrics/semantic_conventions/http-metrics.md#http-client).
     */
    fun setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics: Boolean) {
        this.emitExperimentalHttpClientMetrics = emitExperimentalHttpClientMetrics
    }

    fun emitExperimentalHttpClientMetrics(): Boolean = emitExperimentalHttpClientMetrics

    override fun install(ctx: InstallationContext) {
        HttpUrlConnectionSingletons.configure(this, ctx.openTelemetry)
    }

    /**
     * Configures the connection inactivity timeout in milliseconds.
     *
     * This timeout defines the time that should have elapsed since the connection was last
     * active. It is used by the idle connection harvester thread to find idle connections that
     * should be reported. To schedule the idle connection harvester follow instructions defined in
     * the [README](https://github.com/open-telemetry/opentelemetry-android/blob/96ea4aa9fe709838a91811deeb85b0f1baceb8cd/instrumentation/httpurlconnection/README.md#scheduling-harvester-thread).
     * If the specified timeout is negative, an `IllegalArgumentException` will be thrown.
     *
     * Default value: "10000"
     *
     * @param timeoutMs the timeout period in milliseconds. Must be non-negative.
     * @throws IllegalArgumentException if `timeoutMs` is negative.
     */
    fun setConnectionInactivityTimeoutMs(timeoutMs: Long) {
        require(timeoutMs >= 0) { "timeoutMs must be non-negative" }
        this.reportIdleConnectionInterval = timeoutMs
    }

    /**
     * Configures the connection inactivity timeout in milliseconds for testing purposes only.
     *
     * This test only API allows you to set negative values for timeout. For production
     * workflows, `setConnectionInactivityTimeoutMs` API should be used.
     *
     * @param timeoutMsForTesting the timeout period in milliseconds.
     */
    fun setConnectionInactivityTimeoutMsForTesting(timeoutMsForTesting: Long) {
        this.reportIdleConnectionInterval = timeoutMsForTesting
    }

    val reportIdleConnectionRunnable: Runnable
        /**
         * Returns a runnable that can be scheduled to run periodically at a fixed interval to close
         * open spans if connection is left idle for connectionInactivityTimeoutMs duration.
         * connectionInactivityTimeoutMs can be obtained via getReportIdleConnectionInterval() API.
         *
         * @return The idle connection reporting runnable
         */
        get() =
            object : Runnable {
                override fun run() {
                    HttpUrlReplacements.reportIdleConnectionsOlderThan(reportIdleConnectionInterval)
                }

                override fun toString(): String = "ReportIdleConnectionsRunnable"
            }

    override val name: String = "httpurlconnection"
}
