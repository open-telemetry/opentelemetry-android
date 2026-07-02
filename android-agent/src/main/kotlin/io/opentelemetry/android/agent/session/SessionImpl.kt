/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.session

import io.opentelemetry.android.session.Session

internal class SessionImpl(
    override val id: String,
    override val startTimestamp: Long,
) : Session

internal val invalidSession = SessionImpl("", -1)
