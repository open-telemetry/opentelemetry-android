/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.session.SessionObserver
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.android.session.SessionPublisher
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Type-safe config DSL that controls how sessions should behave.
 */
@OpenTelemetryDslMarker
class SessionConfiguration internal constructor() {
    /**
     * The maximum duration which a session can remain open in the background before it
     * automatically expires.
     */
    var backgroundInactivityTimeout: Duration = 15.minutes

    /**
     * The maximum duration which a session can remain open before it automatically expires.
     */
    var maxLifetime: Duration = 4.hours

    /**
     * A custom [SessionProvider] that controls the session ID. When this is set
     * [backgroundInactivityTimeout] and [maxLifetime] have no effect
     * and the supplied provider owns the session lifecycle entirely. Any [observers] are registered
     * on the supplied provider only if it also implements [SessionPublisher].
     */
    var provider: SessionProvider? = null

    private var observersList: MutableList<SessionObserver> = mutableListOf()

    internal fun getObservers(): List<SessionObserver> = observersList.toList()

    fun observers(vararg observers: SessionObserver) {
        observersList.addAll(observers)
    }
}
