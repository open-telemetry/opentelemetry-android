/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

import android.view.Window

interface ToggleableTracker {
    fun startTracking(window: Window)

    fun stopTracking()
}
