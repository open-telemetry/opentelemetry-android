/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.common.Clock

internal object NoopOpenTelemetryRum : OpenTelemetryRum {
    override val openTelemetry: OpenTelemetry = OpenTelemetry.noop()
    override val sessionProvider: SessionProvider = SessionProvider.getNoop()
    override val clock: Clock = Clock.getDefault()

    override fun emitEvent(
        eventName: String,
        body: String,
        attributes: Attributes,
    ) {
    }

    override fun shutdown() {
    }
}
