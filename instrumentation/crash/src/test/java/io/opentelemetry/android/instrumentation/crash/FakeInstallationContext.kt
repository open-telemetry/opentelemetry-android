/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import android.app.Application
import io.mockk.mockk
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.common.Clock

internal fun fakeInstallationContext(openTelemetry: OpenTelemetry): InstallationContext {
    val ctx = mockk<Application>(relaxed = true)
    return InstallationContext(
        context = ctx,
        openTelemetry = openTelemetry,
        sessionProvider = SessionProvider.getNoop(),
        clock = Clock.getDefault(),
    )
}
