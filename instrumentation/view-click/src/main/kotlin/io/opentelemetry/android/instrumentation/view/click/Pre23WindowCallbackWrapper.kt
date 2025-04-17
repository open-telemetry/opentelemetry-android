/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.view.click

import android.view.Window.Callback

class Pre23WindowCallbackWrapper(
    callback: Callback,
) : DefaultWindowCallback(callback)
