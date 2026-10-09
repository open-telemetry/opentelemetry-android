/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.InstallConstraints
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.random.Random

private class NonComparableInstrumentation(
    override val name: String,
) : AndroidInstrumentation {
    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
    }

    override fun toString(): String = name
}

class InstrumentationInstallOrderStressTest {
    @Test
    fun `satisfiable constraints are satisfied in every discovery order`() {
        val all =
            listOf(
                FakeInstrumentation("session", InstallConstraints(installFirst = true)),
                FakeInstrumentation(
                    "native-crash",
                    InstallConstraints(installFirst = true, installAfter = setOf("session")),
                ),
                FakeInstrumentation("x", InstallConstraints(installAfter = setOf("absent"))),
                FakeInstrumentation("p", InstallConstraints(installBefore = setOf("x"))),
                FakeInstrumentation("q", InstallConstraints(installAfter = setOf("absent"))),
                FakeInstrumentation("last", InstallConstraints(installLast = true)),
                NonComparableInstrumentation("plain"),
            )

        val violations =
            permutations(all)
                .map { it to it.sortedForInstall { warning -> throw AssertionError(warning) } }
                .filter { (_, sorted) -> !satisfiesConstraints(sorted) }
                .map { (input, sorted) -> "$input -> $sorted" }

        assertThat(violations.size).describedAs(violations.take(5).joinToString("\n")).isZero()
    }

    @Test
    fun `500 instrumentations with sparse constraints`() {
        assertAlwaysSorts(maxNamed = 3, firstOrLastPercent = 20, plainPercent = 20)
    }

    @Test
    fun `500 instrumentations with dense constraints`() {
        assertAlwaysSorts(maxNamed = 100, firstOrLastPercent = 20, plainPercent = 0)
    }

    @Test
    fun `500 instrumentations mostly competing for first and last`() {
        assertAlwaysSorts(maxNamed = 1, firstOrLastPercent = 90, plainPercent = 5)
    }

    @Test
    fun `500 instrumentations with only named constraints`() {
        assertAlwaysSorts(maxNamed = 10, firstOrLastPercent = 0, plainPercent = 0)
    }

    private fun assertAlwaysSorts(
        maxNamed: Int,
        firstOrLastPercent: Int,
        plainPercent: Int,
        size: Int = 500,
        seeds: Int = 50,
    ) {
        repeat(seeds) { seed ->
            val instrumentations =
                randomInstrumentations(Random(seed), size, maxNamed, firstOrLastPercent, plainPercent)

            val sorted = instrumentations.sortedForInstall {}

            assertThat(sorted).describedAs("seed $seed").containsExactlyInAnyOrderElementsOf(instrumentations)
        }
    }

    private fun randomInstrumentations(
        random: Random,
        size: Int,
        maxNamed: Int,
        firstOrLastPercent: Int,
        plainPercent: Int,
    ): List<AndroidInstrumentation> {
        val names = List(size) { "i$it" }
        return names.map { name ->
            if (random.nextInt(100) < plainPercent) {
                NonComparableInstrumentation(name)
            } else {
                val named = names.shuffled(random).take(random.nextInt(maxNamed + 1)) - name
                val split = random.nextInt(named.size + 1)
                val firstOrLast = random.nextInt(100) < firstOrLastPercent
                val first = firstOrLast && random.nextBoolean()
                FakeInstrumentation(
                    name,
                    InstallConstraints(
                        installBefore = named.take(split).toSet(),
                        installAfter = named.drop(split).toSet(),
                        installFirst = first,
                        installLast = firstOrLast && !first,
                    ),
                )
            }
        }
    }

    private fun satisfiesConstraints(sorted: List<AndroidInstrumentation>): Boolean =
        sorted.indices.all { i ->
            (i + 1 until sorted.size).none { j -> wantsToBeBefore(sorted[j], sorted[i]) }
        }

    @Suppress("UNCHECKED_CAST")
    private fun wantsToBeBefore(
        a: AndroidInstrumentation,
        b: AndroidInstrumentation,
    ): Boolean =
        ((a as? Comparable<AndroidInstrumentation>)?.compareTo(b) ?: 0) < 0 ||
            ((b as? Comparable<AndroidInstrumentation>)?.compareTo(a) ?: 0) > 0

    private fun <T> permutations(items: List<T>): List<List<T>> =
        if (items.size <= 1) {
            listOf(items)
        } else {
            items.flatMap { item -> permutations(items - item).map { listOf(item) + it } }
        }
}
