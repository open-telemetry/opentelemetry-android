/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

import android.app.Application
import io.opentelemetry.android.internal.services.ServiceManager
import io.opentelemetry.api.OpenTelemetry

data class InstallationContext(
    val application: Application,
    val openTelemetry: OpenTelemetry,
    val serviceManager: ServiceManager,
)
