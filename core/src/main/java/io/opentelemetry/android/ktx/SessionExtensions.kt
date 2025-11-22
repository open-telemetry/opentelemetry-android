/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.ktx

import io.opentelemetry.android.annotations.Incubating
import io.opentelemetry.android.session.SessionIdentifiers
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.logs.LogRecordBuilder
import io.opentelemetry.api.trace.Span
import io.opentelemetry.sdk.logs.ReadWriteLogRecord

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
 * Extension function for different telemetry signal types to set session identifiers.
 */
private inline fun <T> T.setSessionIdentifiersWith(
    sessionProvider: SessionProvider,
    setAttribute: (AttributeKey<String>, String) -> Unit,
): T {
    setAttribute(SessionIdentifiers.SESSION_ID, sessionProvider.getSessionId())

    // Only add the previous session id if it is not blank. If it is present, OpenTelemetry
    // considers the previous session as having completed.
    val previousSessionId = sessionProvider.getPreviousSessionId()
    if (previousSessionId.isNotBlank()) {
        setAttribute(SessionIdentifiers.SESSION_PREVIOUS_ID, previousSessionId)
    }

    return this
}

/**
 * Sets the session identifiers in the attributes builder.
 * @param sessionProvider the session provider.
 * @return the attributes builder for which the function was called on.
 */
@Incubating
fun AttributesBuilder.setSessionIdentifiersWith(sessionProvider: SessionProvider): AttributesBuilder {
    addIfValueIsNotNullBlankAndEmpty(
        SessionIdentifiers.SESSION_ID.key,
        sessionProvider.getSessionId(),
    )
    addIfValueIsNotNullBlankAndEmpty(
        SessionIdentifiers.SESSION_PREVIOUS_ID.key,
        sessionProvider.getPreviousSessionId(),
    )

    return this
}

/**
 * Adds the key-value pair into the attributes map if the value is not null, not blank, nor empty.
 * @param key the key corresponding to the value.
 * @param value the value corresponding to the key.
 * @return the attributes builder for which the function was called on.
 */
@Incubating
fun AttributesBuilder.addIfValueIsNotNullBlankAndEmpty(
    key: String,
    value: String?,
): AttributesBuilder {
    if (!value.isNullOrBlank() && value.isNotEmpty()) {
        put(key, value)
    }
    return this
}
