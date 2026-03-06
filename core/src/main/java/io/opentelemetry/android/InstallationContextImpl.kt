/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.app.Application
import android.content.Context
import io.opentelemetry.android.instrumentation.InstallationContext
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.common.Clock

internal class InstallationContextImpl(
    override val context: Context,
    override val openTelemetry: OpenTelemetry,
    override val sessionProvider: SessionProvider,
    override val clock: Clock,
) : InstallationContext {
    override val application: Application? = context as? Application
}
