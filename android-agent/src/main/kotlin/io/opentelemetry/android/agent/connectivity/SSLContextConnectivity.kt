package io.opentelemetry.android.agent.connectivity

import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

data class SSLContextConnectivity(
    val sslContext: SSLContext,
    val sslX509TrustManager: X509TrustManager
)