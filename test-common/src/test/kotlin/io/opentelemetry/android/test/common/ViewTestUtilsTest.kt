package io.opentelemetry.android.test.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.Test

class ViewTestUtilsTest {

    @Test
    fun testGetDestinationHorizontal() {
        val pointA = arrayOf(20f, 20f)
        val expectedPointB = arrayOf(40f, 20f)
        val (pointBx, pointBy) = getDestinationPoint(pointA, 20, 0.0)
        assertThat(pointBx).isCloseTo(expectedPointB[0], within(1f))
        assertThat(pointBy).isCloseTo(expectedPointB[1], within(1f))
    }

    @Test
    fun testGetDestinationVertical() {
        val pointA = arrayOf(20f, 20f)
        val expectedPointB = arrayOf(20f, 40f)
        val (pointBx, pointBy) = getDestinationPoint(pointA, 20, 90.0)
        assertThat(pointBx).isCloseTo(expectedPointB[0], within(1f))
        assertThat(pointBy).isCloseTo(expectedPointB[1], within(1f))
    }

    @Test
    fun testGetDestinationNonCardinalAngle() {
        val pointA = arrayOf(20f, 20f)
        val expectedPointB = arrayOf(34.14f, 34.14f)
        val (pointBx, pointBy) = getDestinationPoint(pointA, 20, 45.0)
        assertThat(pointBx).isCloseTo(expectedPointB[0], within(1f))
        assertThat(pointBy).isCloseTo(expectedPointB[1], within(1f))
    }

    @Test
    fun testGetDestinationNonCardinalAngleNegativeDirection() {
        val pointA = arrayOf(20f, 20f)
        val expectedPointB = arrayOf(5.86f, 5.86f)
        val (pointBx, pointBy) = getDestinationPoint(pointA, 20, 225.0)
        assertThat(pointBx).isCloseTo(expectedPointB[0], within(1f))
        assertThat(pointBy).isCloseTo(expectedPointB[1], within(1f))
    }
}