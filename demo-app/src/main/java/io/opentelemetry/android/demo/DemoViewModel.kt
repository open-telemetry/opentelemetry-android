/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class DemoViewModel : ViewModel() {
    val sessionIdState = MutableStateFlow("? unknown ?")
}
