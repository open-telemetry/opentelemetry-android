/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DemoViewModel : ViewModel() {
    val sessionIdState = MutableStateFlow("? unknown ?")

    init {
        viewModelScope.launch {
            while (true) {
                delay(5000)
                // TODO: Do some work here maybe
            }
        }
    }

    private fun updateSession() {
        // TODO
    }
}
