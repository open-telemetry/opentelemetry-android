/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

/**
 * Experimental RUM operations that may change or be removed.
 *
 * Instances returned by [io.opentelemetry.android.agent.OpenTelemetryRumInitializer.initialize]
 * implement this interface. Use a safe cast (`as?`) with other [OpenTelemetryRum] instances.
 * Core builder results and instrumentation callback arguments do not implement this interface;
 * the initializer returns a wrapper, not the same instance passed to instrumentation callbacks.
 */
@Incubating
interface IncubatingOpenTelemetryRum : OpenTelemetryRum {
    /**
     * Immediately replaces the agent's current session with a new one linked to the previous session.
     * Existing spans and emitted logs keep their original session IDs. New spans and logs use the
     * replacement session unless their existing session ID is preserved by the configured processors.
     * Automatic expiry still applies to the replacement session. Call only before [shutdown].
     *
     * Storage and observer callbacks run synchronously on the calling thread. If one throws, the
     * exception propagates after the in-memory session has changed. Remaining callbacks are skipped
     * without retrying, so storage and session events may be incomplete. A request made while another
     * transition is being delivered returns false without queuing a replacement, including calls
     * from a session observer.
     *
     * Returns false when a custom session provider is configured, leaving that provider unchanged.
     *
     * @return true when the replacement completes, or false for a custom provider or a busy transition.
     */
    fun startNewSession(): Boolean
}
