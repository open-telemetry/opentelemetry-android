/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import io.mockk.mockk
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry

internal fun fakeInstallationContext(openTelemetry: OpenTelemetry): InstallationContext =
    InstallationContext(
        application = mockk(relaxed = true),
        openTelemetry = openTelemetry,
        sessionProvider = SessionProvider.getNoop(),
    )
