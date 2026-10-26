/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.internal

import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import io.opentelemetry.android.instrumentation.OrderedAndroidInstrumentation
import java.util.ArrayDeque

/**
 * Sorts instrumentations topologically based on ordering constraints defined by [OrderedAndroidInstrumentation].
 *
 * Instrumentations without ordering constraints retain their stable input (discovery) order.
 * If a cycle is detected, a warning is emitted via [warn] and remaining unvisited instrumentations
 * are appended in their original order so startup never crashes.
 */
internal object InstrumentationSorter {
    fun sort(
        instrumentations: List<AndroidInstrumentation>,
        warn: (String) -> Unit = {},
    ): List<AndroidInstrumentation> {
        if (instrumentations.size <= 1) {
            return instrumentations
        }

        val nameToIndices = mutableMapOf<String, MutableList<Int>>()
        instrumentations.forEachIndexed { index, inst ->
            nameToIndices.getOrPut(inst.name) { mutableListOf() }.add(index)
        }

        val (outgoingEdges, inDegree) = buildDependencyGraph(instrumentations, nameToIndices)

        return executeKahnSort(instrumentations, outgoingEdges, inDegree, warn)
    }

    private fun buildDependencyGraph(
        instrumentations: List<AndroidInstrumentation>,
        nameToIndices: Map<String, List<Int>>,
    ): Pair<Array<MutableSet<Int>>, IntArray> {
        val size = instrumentations.size
        val outgoingEdges = Array(size) { mutableSetOf<Int>() }
        val inDegree = IntArray(size)

        addInstallFirstEdges(instrumentations, outgoingEdges, inDegree)

        for (i in 0 until size) {
            val inst = instrumentations[i]
            if (inst is OrderedAndroidInstrumentation) {
                addNamedEdges(i, inst.installAfter, isAfter = true, nameToIndices, outgoingEdges, inDegree)
                addNamedEdges(i, inst.installBefore, isAfter = false, nameToIndices, outgoingEdges, inDegree)
            }
        }

        return Pair(outgoingEdges, inDegree)
    }

    private fun addInstallFirstEdges(
        instrumentations: List<AndroidInstrumentation>,
        outgoingEdges: Array<MutableSet<Int>>,
        inDegree: IntArray,
    ) {
        val firstIndices = mutableListOf<Int>()
        val nonFirstIndices = mutableListOf<Int>()
        for (i in instrumentations.indices) {
            val inst = instrumentations[i]
            if (inst is OrderedAndroidInstrumentation && inst.installFirst) {
                firstIndices.add(i)
            } else {
                nonFirstIndices.add(i)
            }
        }

        for (first in firstIndices) {
            for (other in nonFirstIndices) {
                if (outgoingEdges[first].add(other)) {
                    inDegree[other]++
                }
            }
        }
    }

    private fun addNamedEdges(
        sourceIndex: Int,
        targets: Set<String>,
        isAfter: Boolean,
        nameToIndices: Map<String, List<Int>>,
        outgoingEdges: Array<MutableSet<Int>>,
        inDegree: IntArray,
    ) {
        for (targetName in targets) {
            val matchingIndices = nameToIndices[targetName] ?: continue
            for (targetIndex in matchingIndices) {
                if (targetIndex != sourceIndex) {
                    val from = if (isAfter) targetIndex else sourceIndex
                    val to = if (isAfter) sourceIndex else targetIndex
                    if (outgoingEdges[from].add(to)) {
                        inDegree[to]++
                    }
                }
            }
        }
    }

    private fun executeKahnSort(
        instrumentations: List<AndroidInstrumentation>,
        outgoingEdges: Array<MutableSet<Int>>,
        inDegree: IntArray,
        warn: (String) -> Unit,
    ): List<AndroidInstrumentation> {
        val available = ArrayDeque<Int>()
        val visited = BooleanArray(instrumentations.size)

        for (i in instrumentations.indices) {
            if (inDegree[i] == 0) {
                available.add(i)
            }
        }

        val sorted = mutableListOf<AndroidInstrumentation>()

        while (available.isNotEmpty()) {
            val current = available.removeFirst()
            visited[current] = true
            sorted.add(instrumentations[current])

            for (neighbor in outgoingEdges[current]) {
                inDegree[neighbor]--
                if (inDegree[neighbor] == 0) {
                    available.add(neighbor)
                }
            }
        }

        if (sorted.size < instrumentations.size) {
            val remainingNames = mutableListOf<String>()
            for (i in instrumentations.indices) {
                if (!visited[i]) {
                    sorted.add(instrumentations[i])
                    remainingNames.add(instrumentations[i].name)
                }
            }
            warn("Cycle detected in instrumentation ordering constraints. Remaining in discovery order: $remainingNames")
        }

        return sorted
    }
}
