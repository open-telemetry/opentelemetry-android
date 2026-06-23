package io.opentelemetry.android.instrumentation.view.scale

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ViewScaleInstrumentationTest {

    @Test
    fun scale_test() {
        // Two pointers - get start and end points
        //Start in view center
        // Move outwards - within view
        // Let go
    }

    @Test
    fun scale_that_goes_outside_view_bounds_should_still_capture() {
        // Two pointers - get start and end points
        //Start in view center
        // Move outwards - within view
        // Move outwards - out of bounds of view
        // Let go
    }
}