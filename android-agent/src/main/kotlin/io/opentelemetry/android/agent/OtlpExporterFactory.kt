/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.connectivity.Compression
import io.opentelemetry.android.agent.connectivity.EndpointConnectivity
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter

@OptIn(Incubating::class)
internal object OtlpExporterFactory {
    fun createHttpSpanExporter(endpoint: EndpointConnectivity): OtlpHttpSpanExporter =
        OtlpHttpSpanExporter
            .builder()
            .setEndpoint(endpoint.getUrl())
            .setHeaders(endpoint::getHeaders)
            .setCompression(endpoint.getCompression().upstreamName())
            .apply {
                endpoint.getClientTls()?.let { setClientTls(it.privateKeyPem, it.certificatePem) }
                endpoint.getSslContext()?.let { setSslContext(it.sslContext, it.sslX509TrustManager) }
            }.build()

    fun createHttpLogExporter(endpoint: EndpointConnectivity): OtlpHttpLogRecordExporter =
        OtlpHttpLogRecordExporter
            .builder()
            .setEndpoint(endpoint.getUrl())
            .setHeaders(endpoint::getHeaders)
            .setCompression(endpoint.getCompression().upstreamName())
            .apply {
                endpoint.getClientTls()?.let { setClientTls(it.privateKeyPem, it.certificatePem) }
                endpoint.getSslContext()?.let { setSslContext(it.sslContext, it.sslX509TrustManager) }
            }.build()

    fun createHttpMetricExporter(endpoint: EndpointConnectivity): OtlpHttpMetricExporter =
        OtlpHttpMetricExporter
            .builder()
            .setEndpoint(endpoint.getUrl())
            .setHeaders(endpoint::getHeaders)
            .setCompression(endpoint.getCompression().upstreamName())
            .apply {
                endpoint.getClientTls()?.let { setClientTls(it.privateKeyPem, it.certificatePem) }
                endpoint.getSslContext()?.let { setSslContext(it.sslContext, it.sslX509TrustManager) }
            }.build()

    fun createGrpcSpanExporter(endpoint: EndpointConnectivity): OtlpGrpcSpanExporter =
        OtlpGrpcSpanExporter
            .builder()
            .setEndpoint(endpoint.getUrl())
            .setHeaders(endpoint::getHeaders)
            .setCompression(endpoint.getCompression().upstreamName())
            .apply {
                endpoint.getClientTls()?.let { setClientTls(it.privateKeyPem, it.certificatePem) }
                endpoint.getSslContext()?.let { setSslContext(it.sslContext, it.sslX509TrustManager) }
            }.build()

    fun createGrpcLogExporter(endpoint: EndpointConnectivity): OtlpGrpcLogRecordExporter =
        OtlpGrpcLogRecordExporter
            .builder()
            .setEndpoint(endpoint.getUrl())
            .setHeaders(endpoint::getHeaders)
            .setCompression(endpoint.getCompression().upstreamName())
            .apply {
                endpoint.getClientTls()?.let { setClientTls(it.privateKeyPem, it.certificatePem) }
                endpoint.getSslContext()?.let { setSslContext(it.sslContext, it.sslX509TrustManager) }
            }.build()

    fun createGrpcMetricExporter(endpoint: EndpointConnectivity): OtlpGrpcMetricExporter =
        OtlpGrpcMetricExporter
            .builder()
            .setEndpoint(endpoint.getUrl())
            .setHeaders(endpoint::getHeaders)
            .setCompression(endpoint.getCompression().upstreamName())
            .apply {
                endpoint.getClientTls()?.let { setClientTls(it.privateKeyPem, it.certificatePem) }
                endpoint.getSslContext()?.let { setSslContext(it.sslContext, it.sslX509TrustManager) }
            }.build()

    private fun Compression.upstreamName(): String =
        when (this) {
            Compression.GZIP -> "gzip"
            else -> "none"
        }
}
