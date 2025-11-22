/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes

/**
 * Data class representing session identifiers.
 *
 * @param currentSessionId the current session ID.
 * @param previousSessionId the previous session ID.
 */
@Incubating
data class SessionIdentifiers(
    val currentSessionId: String,
    val previousSessionId: String,
) {
    companion object {
        /**
         * the attribute key for the current session ID.
         *
         * This references the OpenTelemetry semantic convention for session identifiers.
         * @see SessionIncubatingAttributes.SESSION_ID
         */
        @JvmField
        val SESSION_ID: AttributeKey<String> = SessionIncubatingAttributes.SESSION_ID

        /**
         * the attribute key for the previous session ID.
         *
         * This references the OpenTelemetry semantic convention for previous session identifiers.
         * Used to correlate telemetry across session boundaries.
         * @see SessionIncubatingAttributes.SESSION_PREVIOUS_ID
         */
        @JvmField
        val SESSION_PREVIOUS_ID: AttributeKey<String> = SessionIncubatingAttributes.SESSION_PREVIOUS_ID
    }
}
