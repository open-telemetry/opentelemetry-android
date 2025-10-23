package io.opentelemetry.android.instrumentation.screen_orientation

import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.android.instrumentation.InstallationContext
import org.junit.Before
import org.junit.Test

class ScreenOrientationInstrumentationTest {
    private lateinit var sut: ScreenOrientationInstrumentation

    private val context = mockk<InstallationContext>(relaxed = true)

    @Before
    fun setup() {
        sut = ScreenOrientationInstrumentation()
    }

    @Test
    fun `should register component callbacks on install`() {
        // when
        sut.install(context)

        // then
        verify {
            context.context.applicationContext.registerComponentCallbacks(any<ScreenOrientationDetector>())
        }
    }

    @Test
    fun `should unregister component callbacks on uninstall`() {
        // given
        sut.install(context)

        // when
        sut.uninstall(context)

        // then
        verify {
            context.context.applicationContext.unregisterComponentCallbacks(any<ScreenOrientationDetector>())
        }
    }
}
