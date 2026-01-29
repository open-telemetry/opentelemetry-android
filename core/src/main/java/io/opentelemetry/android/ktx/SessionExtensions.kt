/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.ktx

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.android.session.SessionProvider.Companion.NO_SESSION_ID
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.logs.ReadWriteLogRecord
import io.opentelemetry.semconv.incubating.SessionIncubatingAttributes

/**
 * Adds the session identifiers from the [sessionProvider] to this [Span]'s attributes.
 * @param sessionProvider the provider that supplies the session identifiers.
 * @return this [Span] for which the session identifiers have been added.
 */
fun Span.setSessionIdentifiersWith(sessionProvider: SessionProvider): Span = setSessionIdentifiersWith(sessionProvider, ::setAttribute)

/**
 * Extension function for [LogRecordBuilder] to set session identifiers.
 * @param sessionProvider the [SessionProvider] to use for retrieving session identifiers.
 * @return the [LogRecordBuilder] with session identifiers set.
 */
fun LogRecordBuilder.setSessionIdentifiersWith(sessionProvider: SessionProvider): LogRecordBuilder =
    setSessionIdentifiersWith(sessionProvider, ::setAttribute)

/**
 * Extension function for [ReadWriteLogRecord] to set session identifiers.
 *
 * This is used by log record processors to add session identifiers to log records
 * during the processing pipeline.
 *
 * @param sessionProvider the [SessionProvider] to use for retrieving session identifiers.
 * @return the [ReadWriteLogRecord] with session identifiers set.
 */
fun ReadWriteLogRecord.setSessionIdentifiersWith(sessionProvider: SessionProvider): ReadWriteLogRecord =
    setSessionIdentifiersWith(sessionProvider, ::setAttribute)

/**
 * Sets the session identifiers in the attributes builder.
 * @param sessionProvider the session provider.
 * @return the attributes builder for which the function was called on.
 */
@Incubating
fun AttributesBuilder.setSessionIdentifiersWith(sessionProvider: SessionProvider): AttributesBuilder =
    setSessionIdentifiersWith(sessionProvider, ::put)

/**
 * Determines if an attribute value should be added based on null, blank, and empty checks.
 */
private fun shouldAddSessionIdentifier(value: String?): Boolean = !value.isNullOrBlank() && value != NO_SESSION_ID

/**
 * Extension function for different telemetry signal types to set session identifiers.
 */
@OptIn(Incubating::class)
private inline fun <T> T.setSessionIdentifiersWith(
    sessionProvider: SessionProvider,
    setAttribute: (AttributeKey<String>, String) -> Unit,
): T {
    val currentSessionId = sessionProvider.getSessionId()

    // Only add session identifiers if there's an active session
    if (shouldAddSessionIdentifier(currentSessionId)) {
        setAttribute(SessionIncubatingAttributes.SESSION_ID, currentSessionId)

        // Only add the previous session id if available
        val previousSessionId = sessionProvider.getPreviousSessionId()
        if (shouldAddSessionIdentifier(previousSessionId)) {
            setAttribute(SessionIncubatingAttributes.SESSION_PREVIOUS_ID, previousSessionId)
        }
    }

    return this
}
