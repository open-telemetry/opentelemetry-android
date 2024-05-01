package io.opentelemetry.android.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CoordinatesViewModel : ViewModel() {
    val distanceState = MutableStateFlow("0.0")
    val elevationState = MutableStateFlow("0.0")
    private var distance = 0f
    private var elevation = 0f
    private val tracer = OtelSampleApplication.tracer("bb.distance")!!

    init {
        viewModelScope.launch {
            while (true) {
                delay(500)
                updateDistance()
            }
        }
        viewModelScope.launch {
            delay(1000)
            while (true) {
                delay(500)
                updateElevation()
            }
        }
    }

    private fun updateDistance() {
        distance += 0.003f
        distanceState.value = String.format("%.2f", distance)
        sendTrace("distance", distance)
    }

    private fun updateElevation() {
        elevation += 0.005f
        elevationState.value = String.format("%.2f", elevation)
        sendTrace("elevation", elevation)
    }

    private fun sendTrace(type: String, value: Float) {
        // A metric should be a better fit for this use case, but due to presentation limitations we're using spans.

        tracer.spanBuilder(type).setAttribute("value", value.toDouble()).startSpan().end()
    }
}