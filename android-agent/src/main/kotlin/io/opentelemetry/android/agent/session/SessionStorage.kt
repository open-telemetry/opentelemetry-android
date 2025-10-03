/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.session.Session

internal interface SessionStorage {
    fun get(): Session

    fun save(newSession: Session)
}
