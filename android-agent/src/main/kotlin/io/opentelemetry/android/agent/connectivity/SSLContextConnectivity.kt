/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.connectivity

import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class SSLContextConnectivity(
    val sslContext: SSLContext,
    val sslX509TrustManager: X509TrustManager,
)
