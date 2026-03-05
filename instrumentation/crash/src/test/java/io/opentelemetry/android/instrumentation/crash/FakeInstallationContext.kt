/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.common.Clock

internal fun fakeInstallationContext(openTelemetry: OpenTelemetry): InstallationContext {
    val ctx = mockk<Application>(relaxed = true)
    return mockk<InstallationContext>().also {
        every { it.context } returns ctx
        every { it.openTelemetry } returns openTelemetry
        every { it.sessionProvider } returns SessionProvider.getNoop()
        every { it.clock } returns Clock.getDefault()
        every { it.application } returns ctx
    }
}
