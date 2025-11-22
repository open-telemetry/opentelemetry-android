/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.demo

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class DemoViewModel : ViewModel() {
    val sessionIdState = MutableStateFlow("? unknown ?")
    private val tracer = OtelDemoApplication.tracer("otel.demo")!!

    init {
        // Set initial session ID
        updateSession()
    }

    private fun updateSession() {
        val currentSessionId = OtelDemoApplication.rum?.getRumSessionId()
        if (currentSessionId != null && currentSessionId.isNotEmpty()) {
            sessionIdState.value = currentSessionId
        }
    }

    private fun sendTrace(
        type: String,
        value: Float,
    ) {
        // A metric should be a better fit, but for now we're using spans.
        tracer.spanBuilder(type).setAttribute("value", value.toDouble()).startSpan().end()
    }
}
