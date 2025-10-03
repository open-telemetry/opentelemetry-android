/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.session.Session

internal class InMemorySessionStorage(
    @Volatile var session: Session = Session.NONE,
) : SessionStorage {
    override fun get(): Session = session

    override fun save(newSession: Session) {
        this.session = newSession
    }
}
