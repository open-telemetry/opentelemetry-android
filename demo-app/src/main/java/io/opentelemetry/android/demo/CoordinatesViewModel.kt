package io.opentelemetry.android.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CoordinatesViewModel : ViewModel() {
    val distanceState = MutableStateFlow("0.0")
    val sessionIdState = MutableStateFlow("? unknown ?")
    private var distance = 0f
    private val tracer = OtelSampleApplication.tracer("bb.distance")!!

    init {
        viewModelScope.launch {
            while (true) {
                delay(500)
                updateDistance()
            }
        }
    }

    private fun updateDistance() {
        distance += 0.003f
        distanceState.value = String.format("%.2f", distance)
        sendTrace("distance", distance)
    }

    private fun updateSession(){

    }

    private fun sendTrace(type: String, value: Float) {
        // A metric should be a better fit, but for now we're using spans.
        tracer.spanBuilder(type).setAttribute("value", value.toDouble()).startSpan().end()
    }
}