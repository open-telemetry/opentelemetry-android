/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.app.Application
import android.content.Context
import io.opentelemetry.android.session.SessionProvider
import io.opentelemetry.api.OpenTelemetry

class InstallationContext(
    val context: Context,
    val openTelemetry: OpenTelemetry,
    val sessionProvider: SessionProvider,
) {
    val application: Application? = context as? Application
}
