/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.session

/**
 * A facade contract for retrieving session identifiers.
 */
interface SessionIdFacade {
    /**
     * the current session identifiers.
     */
    val sessionIdentifiers: SessionIdentifiers
}
