/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

interface SessionStorage {
    fun get(): Session

    fun save(newSession: Session)

    class InMemory(
        @Volatile var session: Session = Session.NONE,
    ) : SessionStorage {
        override fun get(): Session = session

        override fun save(newSession: Session) {
            this.session = newSession
        }
    }
}
