import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.test.core.view.PointerCoordsBuilder
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.slot
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit


inline fun <reified T : View> mockView(
    id: Int,
    motionEvent: MotionEvent,
    hitOffset: IntArray = intArrayOf(0, 0),
    clickable: Boolean = true,
    visibility: Int = View.VISIBLE,
    applyOthers: (T) -> Unit = {},
): T {
    val mockView = mockkClass(T::class)
    every { mockView.visibility } returns visibility
    every { mockView.isClickable } returns clickable

    every { mockView.id } returns id
    val location = IntArray(2)

    location[0] = (motionEvent.x + hitOffset[0]).toInt()
    location[1] = (motionEvent.y + hitOffset[1]).toInt()

    val arrayCapturingSlot = slot<IntArray>()
    every { mockView.getLocationInWindow(capture(arrayCapturingSlot)) } answers {
        arrayCapturingSlot.captured[0] = location[0]
        arrayCapturingSlot.captured[1] = location[1]
    }

    every { mockView.x } returns location[0].toFloat()
    every { mockView.y } returns location[1].toFloat()

    every { mockView.width } returns (location[0] + hitOffset[0])
    every { mockView.height } returns (location[1] + hitOffset[1])
    applyOthers.invoke(mockView)

    return mockView
}

private val allowedToolTypes = arrayOf(MotionEvent.TOOL_TYPE_FINGER, MotionEvent.TOOL_TYPE_MOUSE,
    MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER, MotionEvent.TOOL_TYPE_UNKNOWN)

private val allowedButtonStates = arrayOf(
    MotionEvent.BUTTON_PRIMARY, MotionEvent.BUTTON_STYLUS_PRIMARY,
    MotionEvent.BUTTON_SECONDARY, MotionEvent.BUTTON_STYLUS_SECONDARY,
    MotionEvent.BUTTON_TERTIARY,
    MotionEvent.BUTTON_BACK,
    MotionEvent.BUTTON_FORWARD
)

fun getDoubleTapSequence(x: Float, y: Float, toolType: Int = MotionEvent.TOOL_TYPE_FINGER, buttonState: Int = 0,
                                 exceedTimeOut: Boolean = false): Array<MotionEvent> {

    require(toolType in allowedToolTypes) { "Invalid tool type" }

    if(buttonState != 0) {
        require(toolType == MotionEvent.TOOL_TYPE_MOUSE || toolType == MotionEvent.TOOL_TYPE_STYLUS) {
            "Invalid tool type for button state"
        }
        require(buttonState in allowedButtonStates) { "Invalid button state" }
    }

    val initialTime = SystemClock.uptimeMillis()

    val pointerProperties = MotionEvent.PointerProperties()
    pointerProperties.id = 0
    pointerProperties.toolType = toolType

    val pointerCoords = PointerCoordsBuilder.newBuilder().setCoords(x, y).build()

    if(exceedTimeOut) {
        val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout()

        return arrayOf(
            MotionEvent.obtain(initialTime, initialTime,
                MotionEvent.ACTION_DOWN, 1, arrayOf(pointerProperties),
                arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
                0, 0, 0, 0),
            MotionEvent.obtain(initialTime, initialTime + 300L,
                MotionEvent.ACTION_UP, 1, arrayOf(pointerProperties),
                arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
                0, 0, 0, 0),

            MotionEvent.obtain(
                initialTime + 400L + doubleTapTimeout, initialTime + 500L + doubleTapTimeout,
                MotionEvent.ACTION_DOWN, 1, arrayOf(pointerProperties),
                arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
                0, 0, 0, 0),

            MotionEvent.obtain(
                initialTime + 600L + doubleTapTimeout, initialTime + 700L + doubleTapTimeout,
                MotionEvent.ACTION_UP, 1, arrayOf(pointerProperties),
                arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
                0, 0, 0, 0)
        )
    } else {

        return arrayOf(
            MotionEvent.obtain(initialTime, initialTime,
                MotionEvent.ACTION_DOWN, 1, arrayOf(pointerProperties),
                arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
                0, 0, 0, 0),
            MotionEvent.obtain(initialTime, initialTime + 300L,
                MotionEvent.ACTION_UP, 1, arrayOf(pointerProperties),
                arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
                0, 0, 0, 0),

            MotionEvent.obtain(
                initialTime + 400L, initialTime + 500L,
                MotionEvent.ACTION_DOWN, 1, arrayOf(pointerProperties),
                arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
                0, 0, 0, 0),

            MotionEvent.obtain(
                initialTime + 600L, initialTime + 700L,
                MotionEvent.ACTION_UP, 1, arrayOf(pointerProperties),
                arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
                0, 0, 0, 0)
        )
    }
}


fun getSingleTapSequence(x: Float, y: Float, toolType: Int = MotionEvent.TOOL_TYPE_FINGER, buttonState: Int = 0)
        : Array<MotionEvent> {
    require(toolType in allowedToolTypes) {
        "Invalid tool type"
    }

    if(buttonState != 0) {
        require(toolType == MotionEvent.TOOL_TYPE_MOUSE || toolType == MotionEvent.TOOL_TYPE_STYLUS) {
            "Invalid tool type for button state"
        }
        require(buttonState in allowedButtonStates) { "Invalid button state" }
    }

    val initialTime = SystemClock.uptimeMillis()

    val pointerProperties = MotionEvent.PointerProperties()
    pointerProperties.id = 0
    pointerProperties.toolType = toolType

    val pointerCoords = PointerCoordsBuilder.newBuilder().setCoords(x, y).build()
    return arrayOf(
        MotionEvent.obtain(initialTime, initialTime,
            MotionEvent.ACTION_DOWN, 1, arrayOf(pointerProperties),
            arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
            0, 0, 0, 0),

        MotionEvent.obtain(initialTime, initialTime + 100L,
            MotionEvent.ACTION_UP, 1, arrayOf(pointerProperties),
            arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
            0, 0, 0, 0)
    )
}

fun getLongPressSequence(x: Float, y: Float, toolType: Int = MotionEvent.TOOL_TYPE_FINGER, buttonState: Int = 0,
                         stayDownLongEnough: Boolean = true)
        : Array<MotionEvent> {
    require(toolType in allowedToolTypes) {
        "Invalid tool type"
    }

    if(buttonState != 0) {
        require(toolType == MotionEvent.TOOL_TYPE_MOUSE || toolType == MotionEvent.TOOL_TYPE_STYLUS) {
            "Invalid tool type for button state"
        }
        require(buttonState in allowedButtonStates) { "Invalid button state" }
    }

    val initialTime = SystemClock.uptimeMillis()

    val pointerProperties = MotionEvent.PointerProperties()
    pointerProperties.id = 0
    pointerProperties.toolType = toolType

    val pointerCoords = PointerCoordsBuilder.newBuilder().setCoords(x, y).build()
    val delay = if(stayDownLongEnough) {
//        val allowanceTime = 100L
        ViewConfiguration.getLongPressTimeout()// + allowanceTime
    } else {
        100
    }
    return arrayOf(
        MotionEvent.obtain(initialTime, initialTime,
            MotionEvent.ACTION_DOWN, 1, arrayOf(pointerProperties),
            arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
            0, 0, 0, 0),
        MotionEvent.obtain(initialTime, initialTime + delay,
            MotionEvent.ACTION_UP, 1, arrayOf(pointerProperties),
            arrayOf(pointerCoords), 0, buttonState, 1f, 1f,
            0, 0, 0, 0)
    )
}

fun fastForwardDoubleTapTimeout() {
    ShadowLooper.idleMainLooper(ViewConfiguration.getDoubleTapTimeout().toLong(), TimeUnit.MILLISECONDS)
}

fun fastForwardLongPressTimeout() {
    val allowanceTime = 150L
    ShadowLooper.idleMainLooper(ViewConfiguration.getLongPressTimeout().toLong() + allowanceTime, TimeUnit.MILLISECONDS)
}
