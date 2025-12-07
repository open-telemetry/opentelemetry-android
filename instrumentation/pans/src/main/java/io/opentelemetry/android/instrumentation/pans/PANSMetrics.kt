/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.pans

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder

/**
 * Represents metrics collected from PANS (Per-Application Network Selection) system.
 */
data class PANSMetrics(
    /** Per-app network usage data */
    val appNetworkUsage: List<AppNetworkUsage> = emptyList(),
    /** Network preference changes */
    val preferenceChanges: List<PreferenceChange> = emptyList(),
    /** Network availability status */
    val networkAvailability: List<NetworkAvailability> = emptyList(),
)

/**
 * Represents network usage for a single application.
 */
data class AppNetworkUsage(
    /** Package name of the application */
    val packageName: String,
    /** UID of the application */
    val uid: Int,
    /** Network type (OEM_PAID, OEM_PRIVATE, etc.) */
    val networkType: String,
    /** Bytes transmitted via this network */
    val bytesTransmitted: Long,
    /** Bytes received via this network */
    val bytesReceived: Long,
    /** OpenTelemetry attributes for this metric */
    val attributes: Attributes,
)

/**
 * Represents a network preference change for an application.
 */
data class PreferenceChange(
    /** Package name of the application */
    val packageName: String,
    /** UID of the application */
    val uid: Int,
    /** Previous network preference */
    val oldPreference: String,
    /** New network preference */
    val newPreference: String,
    /** Timestamp of the change */
    val timestamp: Long = System.currentTimeMillis(),
    /** OpenTelemetry attributes for this event */
    val attributes: Attributes,
)

/**
 * Represents the availability of a network.
 */
data class NetworkAvailability(
    /** Network type (OEM_PAID, OEM_PRIVATE, etc.) */
    val networkType: String,
    /** Whether the network is available */
    val isAvailable: Boolean,
    /** Signal strength if available (-1 if N/A) */
    val signalStrength: Int = -1,
    /** OpenTelemetry attributes for this metric */
    val attributes: Attributes,
)

/**
 * Helper function to build attributes for PANS metrics.
 */
internal fun buildPansAttributes(
    packageName: String,
    networkType: String,
    uid: Int,
    additionalBuilder: (AttributesBuilder) -> Unit = {},
): Attributes =
    Attributes
        .builder()
        .put("app_package_name", packageName)
        .put("network_type", networkType)
        .put("uid", uid.toLong())
        .also(additionalBuilder)
        .build()

/**
 * Helper function to build attributes for preference change events.
 */
internal fun buildPreferenceChangeAttributes(
    packageName: String,
    oldPreference: String,
    newPreference: String,
    uid: Int,
): Attributes =
    Attributes
        .builder()
        .put("app_package_name", packageName)
        .put("old_preference", oldPreference)
        .put("new_preference", newPreference)
        .put("uid", uid.toLong())
        .build()

/**
 * Helper function to build attributes for network availability metrics.
 */
internal fun buildNetworkAvailabilityAttributes(
    networkType: String,
    signalStrength: Int = -1,
): Attributes {
    val builder =
        Attributes
            .builder()
            .put("network_type", networkType)

    if (signalStrength >= 0) {
        builder.put("signal_strength", signalStrength.toLong())
    }

    return builder.build()
}
