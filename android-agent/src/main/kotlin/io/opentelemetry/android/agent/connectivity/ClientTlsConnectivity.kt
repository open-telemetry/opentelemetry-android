package io.opentelemetry.android.agent.connectivity

/**
 * The client key and the certificate chain to use for verifying client when TLS is enabled.
 *
 * @param privateKeyPem client key PKCS8 in PEM format.
 * @param certificatePem client certificate chain in PEM format.
 */
class ClientTlsConnectivity(
    val privateKeyPem: ByteArray,
    val certificatePem: ByteArray,
)