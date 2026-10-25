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

        val nameToInst = mutableMapOf<String, AndroidInstrumentation>()
        for (inst in instrumentations) {
            nameToInst[inst.name] = inst
        }

        // Graph: node -> list of dependent nodes that must come AFTER it (edges: u -> v means u before v)
        val outgoingEdges = mutableMapOf<String, MutableSet<String>>()
        val inDegree = mutableMapOf<String, Int>()

        for (inst in instrumentations) {
            outgoingEdges[inst.name] = mutableSetOf()
            inDegree[inst.name] = 0
        }

        // Build edges based on OrderedAndroidInstrumentation constraints
        for (inst in instrumentations) {
            if (inst is OrderedAndroidInstrumentation) {
                // inst must install AFTER other -> edge other -> inst
                for (beforeName in inst.installAfter) {
                    if (nameToInst.containsKey(beforeName) && beforeName != inst.name) {
                        if (outgoingEdges[beforeName]!!.add(inst.name)) {
                            inDegree[inst.name] = inDegree[inst.name]!! + 1
                        }
                    }
                }

                // inst must install BEFORE other -> edge inst -> other
                for (afterName in inst.installBefore) {
                    if (nameToInst.containsKey(afterName) && afterName != inst.name) {
                        if (outgoingEdges[inst.name]!!.add(afterName)) {
                            inDegree[afterName] = inDegree[afterName]!! + 1
                        }
                    }
                }
            }
        }

        // Stable Kahn's algorithm: queue preserving discovery order
        // We find all nodes with inDegree == 0 in original discovery order
        val available = ArrayDeque<String>()
        val visited = mutableSetOf<String>()

        for (inst in instrumentations) {
            if (inDegree[inst.name] == 0) {
                available.add(inst.name)
            }
        }

        val sorted = mutableListOf<AndroidInstrumentation>()

        while (available.isNotEmpty()) {
            val currentName = available.removeFirst()
            visited.add(currentName)
            sorted.add(nameToInst[currentName]!!)

            for (neighbor in outgoingEdges[currentName]!!) {
                val newDegree = inDegree[neighbor]!! - 1
                inDegree[neighbor] = newDegree
                if (newDegree == 0) {
                    available.add(neighbor)
                }
            }
        }

        // Cycle check: if some nodes were not visited, there is a cycle
        if (sorted.size < instrumentations.size) {
            val remaining = instrumentations.filter { !visited.contains(it.name) }
            val remainingNames = remaining.map { it.name }
            warn("Cycle detected in instrumentation ordering constraints. Remaining in discovery order: $remainingNames")
            sorted.addAll(remaining)
        }

        return sorted
    }
}
