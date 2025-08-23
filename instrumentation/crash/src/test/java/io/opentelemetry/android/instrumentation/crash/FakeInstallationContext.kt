/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.crash

import io.mockk.mockk
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider

internal fun fakeInstallationContext(processor: FakeLogRecordExporter): InstallationContext =
    InstallationContext(
        application = mockk(relaxed = true),
        openTelemetry = fakeOpenTelemetry(processor),
        sessionProvider = SessionProvider.getNoop(),
    )
