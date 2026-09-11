/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.Incubating
import io.opentelemetry.android.agent.session.InMemorySessionStorage
import io.opentelemetry.android.agent.session.SessionStorage
import io.opentelemetry.android.session.SessionObserver
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Type-safe config DSL that controls how sessions should behave.
 */
@OpenTelemetryDslMarker
@OptIn(Incubating::class)
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

    internal var sessionStorage: SessionStorage = InMemorySessionStorage()
        private set

    /**
     * Replaces the default in-memory storage without changing session generation, expiry, or
     * observers. This does not restore sessions across launches; see [SessionStorage] for the
     * startup and failure contract. If called more than once, the last storage supplied is used.
     */
    @Incubating
    fun storage(storage: SessionStorage) {
        sessionStorage = storage
    }

    private var observersList: MutableList<SessionObserver> = mutableListOf()

    internal fun getObservers(): List<SessionObserver> = observersList.toList()

    fun observers(vararg observers: SessionObserver) {
        observersList.addAll(observers)
    }
}
