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

        val nameToInst = instrumentations.associateBy { it.name }
        val (outgoingEdges, inDegree) = buildDependencyGraph(instrumentations, nameToInst)

        return executeKahnSort(instrumentations, nameToInst, outgoingEdges, inDegree, warn)
    }

    private fun buildDependencyGraph(
        instrumentations: List<AndroidInstrumentation>,
        nameToInst: Map<String, AndroidInstrumentation>,
    ): Pair<Map<String, MutableSet<String>>, MutableMap<String, Int>> {
        val outgoingEdges = instrumentations.associate { it.name to mutableSetOf<String>() }
        val inDegree = instrumentations.associate { it.name to 0 }.toMutableMap()

        for (inst in instrumentations) {
            if (inst is OrderedAndroidInstrumentation) {
                addEdges(inst, inst.installAfter, isAfter = true, nameToInst, outgoingEdges, inDegree)
                addEdges(inst, inst.installBefore, isAfter = false, nameToInst, outgoingEdges, inDegree)
            }
        }

        return Pair(outgoingEdges, inDegree)
    }

    private fun addEdges(
        inst: AndroidInstrumentation,
        targets: Set<String>,
        isAfter: Boolean,
        nameToInst: Map<String, AndroidInstrumentation>,
        outgoingEdges: Map<String, MutableSet<String>>,
        inDegree: MutableMap<String, Int>,
    ) {
        for (target in targets) {
            if (nameToInst.containsKey(target) && target != inst.name) {
                val from = if (isAfter) target else inst.name
                val to = if (isAfter) inst.name else target
                if (outgoingEdges[from]?.add(to) == true) {
                    inDegree[to] = (inDegree[to] ?: 0) + 1
                }
            }
        }
    }

    private fun executeKahnSort(
        instrumentations: List<AndroidInstrumentation>,
        nameToInst: Map<String, AndroidInstrumentation>,
        outgoingEdges: Map<String, MutableSet<String>>,
        inDegree: MutableMap<String, Int>,
        warn: (String) -> Unit,
    ): List<AndroidInstrumentation> {
        val available = ArrayDeque<String>()
        val visited = mutableSetOf<String>()

        for (inst in instrumentations) {
            if (inDegree[inst.name] == 0) {
                available.add(inst.name)
            }
        }

        val sorted = mutableListOf<AndroidInstrumentation>()

        while (available.isNotEmpty()) {
            val current = available.removeFirst()
            visited.add(current)
            nameToInst[current]?.let { sorted.add(it) }

            outgoingEdges[current]?.forEach { neighbor ->
                val newDegree = (inDegree[neighbor] ?: 1) - 1
                inDegree[neighbor] = newDegree
                if (newDegree == 0) {
                    available.add(neighbor)
                }
            }
        }

        if (sorted.size < instrumentations.size) {
            val remaining = instrumentations.filter { !visited.contains(it.name) }
            val remainingNames = remaining.map { it.name }
            warn("Cycle detected in instrumentation ordering constraints. Remaining in discovery order: $remainingNames")
            sorted.addAll(remaining)
        }

        return sorted
    }
}
