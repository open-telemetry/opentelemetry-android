/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.nativecrash

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import java.time.Instant

class NativeCrashRecoveryStateTest {
    @Test
    fun `factory rejects invalid recovery states`() {
        for (nowMillis in listOf(0L, -1L)) {
            assertThatIllegalArgumentException().isThrownBy {
                NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.MARKER_READ, nowMillis)
            }
        }
        for (invalidRecord in listOf(
            record.copy(signalNumber = 0),
            record.copy(signalNumber = -1),
            record.copy(timestamp = Instant.ofEpochSecond(-1)),
        )) {
            assertThatIllegalArgumentException().isThrownBy {
                NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.DELIVERY_CLAIMED, 2_000, invalidRecord)
            }
        }
        for (phase in NativeCrashRecoveryPhase.entries) {
            for (identity in listOf(null, record)) {
                val valid =
                    when (phase) {
                        NativeCrashRecoveryPhase.MARKER_READ -> identity == null
                        NativeCrashRecoveryPhase.SNAPSHOT_READ, NativeCrashRecoveryPhase.DELIVERY_CLAIMED -> identity != null
                        NativeCrashRecoveryPhase.CLEANUP, NativeCrashRecoveryPhase.ABANDONED -> true
                    }
                if (valid) {
                    val state = NativeCrashRecoveryState.create(phase, 2_000, identity)
                    assertThat(state.isValid()).isTrue()
                } else {
                    assertThatIllegalArgumentException().isThrownBy {
                        NativeCrashRecoveryState.create(phase, 2_000, identity)
                    }
                }
            }
        }
    }

    @Test
    fun `identified recovery state only applies to the exact crash`() {
        val state = NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.DELIVERY_CLAIMED, 2_000, record)
        assertThat(state.hasIdentity()).isTrue()
        assertThat(state.matches(record)).isTrue()
        assertThat(state.appliesTo(record)).isTrue()

        for (other in listOf(
            record.copy(signalNumber = 6),
            record.copy(timestamp = record.timestamp.plusSeconds(1)),
            record.copy(timestamp = record.timestamp.plusNanos(1)),
        )) {
            assertThat(state.matches(other)).isFalse()
            assertThat(state.appliesTo(other)).isFalse()
        }
        for (partial in listOf(
            state.copy(signalNumber = null),
            state.copy(timestampEpochSecond = null),
            state.copy(timestampNano = null),
        )) {
            assertThat(partial.hasIdentity()).isFalse()
        }
    }

    @Test
    fun `marker read state does not apply to newer crashes`() {
        val state = NativeCrashRecoveryState.create(NativeCrashRecoveryPhase.MARKER_READ, record.timestamp.toEpochMilli())
        val recovered = state

        assertThat(recovered.hasIdentity()).isFalse()
        assertThat(recovered.matches(record)).isFalse()
        assertThat(recovered.appliesTo(record.copy(timestamp = record.timestamp.minusNanos(1)))).isTrue()
        assertThat(recovered.appliesTo(record)).isTrue()
        assertThat(recovered.appliesTo(record.copy(timestamp = record.timestamp.plusNanos(1)))).isFalse()
        assertThat(recovered.appliesTo(record.copy(timestamp = record.timestamp.plusMillis(1)))).isFalse()
    }

    private companion object {
        val record = NativeCrashRecord(11, Instant.ofEpochSecond(1_783_598_400))
    }
}
