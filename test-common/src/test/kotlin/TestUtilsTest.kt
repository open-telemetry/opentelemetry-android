/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import org.assertj.core.api.Assertions
import org.junit.Test

class TestUtilsTest {
    @Test
    fun testGetDestinationHorizontal() {
        val pointA = arrayOf(20f, 20f)
        val expectedPointB = arrayOf(40f, 20f)
        val (pointBx, pointBy) = getDestinationPoint(pointA, 20, 0.0)
        Assertions.assertThat(pointBx).isCloseTo(expectedPointB[0], Assertions.within(1f))
        Assertions.assertThat(pointBy).isCloseTo(expectedPointB[1], Assertions.within(1f))
    }

    @Test
    fun testGetDestinationVertical() {
        val pointA = arrayOf(20f, 20f)
        val expectedPointB = arrayOf(20f, 40f)
        val (pointBx, pointBy) = getDestinationPoint(pointA, 20, 90.0)
        Assertions.assertThat(pointBx).isCloseTo(expectedPointB[0], Assertions.within(1f))
        Assertions.assertThat(pointBy).isCloseTo(expectedPointB[1], Assertions.within(1f))
    }

    @Test
    fun testGetDestinationNonCardinalAngle() {
        val pointA = arrayOf(20f, 20f)
        val expectedPointB = arrayOf(34.14f, 34.14f)
        val (pointBx, pointBy) = getDestinationPoint(pointA, 20, 45.0)
        Assertions.assertThat(pointBx).isCloseTo(expectedPointB[0], Assertions.within(1f))
        Assertions.assertThat(pointBy).isCloseTo(expectedPointB[1], Assertions.within(1f))
    }

    @Test
    fun testGetDestinationNonCardinalAngleNegativeDirection() {
        val pointA = arrayOf(20f, 20f)
        val expectedPointB = arrayOf(5.86f, 5.86f)
        val (pointBx, pointBy) = getDestinationPoint(pointA, 20, 225.0)
        Assertions.assertThat(pointBx).isCloseTo(expectedPointB[0], Assertions.within(1f))
        Assertions.assertThat(pointBy).isCloseTo(expectedPointB[1], Assertions.within(1f))
    }
}
