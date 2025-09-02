/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal.services.applifecycle

import io.opentelemetry.android.internal.services.Service

/**
 * This class is internal and not for public use. Its APIs are unstable and can change at any time.
 */
interface AppLifecycle : Service {
    fun registerListener(listener: ApplicationStateListener)
}
