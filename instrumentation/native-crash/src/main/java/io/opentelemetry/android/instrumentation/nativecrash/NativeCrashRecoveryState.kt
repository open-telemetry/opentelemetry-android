/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

internal enum class NativeCrashRecoveryResult {
    COMPLETE,
    RETRY_PENDING,
}

internal sealed interface NativeCrashRead<out T> {
    data class Success<T>(
        val value: T,
    ) : NativeCrashRead<T>

    data object Missing : NativeCrashRead<Nothing>

    data object Malformed : NativeCrashRead<Nothing>

    data object Failed : NativeCrashRead<Nothing>
}

internal enum class NativeCrashRecoveryPhase {
    MARKER_READ,
    SNAPSHOT_READ,
    DELIVERY_CLAIMED,
    CLEANUP,
    ABANDONED,
}

internal data class NativeCrashRecoveryState(
    val phase: NativeCrashRecoveryPhase,
    val attempts: Int,
    val firstAttemptEpochMillis: Long,
    val signalNumber: Int? = null,
    val timestampEpochSecond: Long? = null,
    val timestampNano: Int? = null,
) {
    fun hasIdentity(): Boolean = signalNumber != null && timestampEpochSecond != null && timestampNano != null

    fun matches(record: NativeCrashRecord): Boolean =
        signalNumber == record.signalNumber &&
            timestampEpochSecond == record.timestamp.epochSecond &&
            timestampNano == record.timestamp.nano

    fun appliesTo(record: NativeCrashRecord): Boolean =
        if (hasIdentity()) matches(record) else record.timestamp.toEpochMilli() <= firstAttemptEpochMillis

    companion object {
        fun create(
            phase: NativeCrashRecoveryPhase,
            nowMillis: Long,
            record: NativeCrashRecord? = null,
        ): NativeCrashRecoveryState =
            NativeCrashRecoveryState(
                phase = phase,
                attempts = 0,
                firstAttemptEpochMillis = nowMillis,
                signalNumber = record?.signalNumber,
                timestampEpochSecond = record?.timestamp?.epochSecond,
                timestampNano = record?.timestamp?.nano,
            )
    }
}

internal fun interface NativeCrashRecoveryLock : AutoCloseable {
    override fun close()
}
