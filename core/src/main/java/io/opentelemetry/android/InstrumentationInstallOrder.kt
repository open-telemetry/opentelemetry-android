/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import android.content.Context
import io.opentelemetry.android.instrumentation.AndroidInstrumentation
import java.util.BitSet
import kotlin.math.sign

/**
 * Orders instrumentations for installation according to the preferences of those that implement
 * `Comparable<AndroidInstrumentation>` (typically by delegating to `InstallConstraints`).
 *
 * Preferences only relate some pairs of instrumentations, so this is a stable topological sort
 * rather than a comparison sort: instrumentations without a preference between them keep their
 * relative order. When both claim the same position, a preference about the other instrumentation
 * specifically takes precedence over one held about every instrumentation. Remaining competing
 * preferences and cycles are reported to [warn] and resolved in favor of the existing order.
 */
internal fun List<AndroidInstrumentation>.sortedForInstall(warn: (String) -> Unit): List<AndroidInstrumentation> {
    // Each instrumentation's preference about one nobody names, i.e. the preference it holds about everything.
    val general = IntArray(size) { this[it].preferenceOver(UnorderedEmptyAndroidInstrumentation) }
    // predecessors[j] has bit i set when instrumentation i must install before instrumentation j.
    val predecessors = List(size) { BitSet(size) }
    // Ask every pair once; resolvePreference already combines both sides' views.
    for (i in indices) {
        for (j in i + 1 until size) {
            when (resolvePreference(this[i], this[j], general[i], general[j], warn)) {
                -1 -> predecessors[j].set(i)
                1 -> predecessors[i].set(j)
            }
        }
    }

    val remaining = BitSet(size).apply { set(0, size) }
    return List(size) {
        // Scan in discovery order for the first instrumentation none of whose predecessors remain.
        var next = remaining.nextSetBit(0)
        while (next >= 0 && predecessors[next].intersects(remaining)) {
            next = remaining.nextSetBit(next + 1)
        }
        // Nothing qualified, so there's a cycle: break it at the earliest remaining instrumentation.
        if (next < 0) {
            next = remaining.nextSetBit(0)
            warn("Instrumentation install order contains a cycle, installing '${this[next].name}' early")
        }
        remaining.clear(next)
        this[next]
    }
}

/**
 * Combines the preferences of [a] and [b] about each other: -1 if [a] should be installed first,
 * 1 if [b] should, and 0 if neither has a preference or their preferences compete.
 * [aGeneral] and [bGeneral] are the preferences each holds about every instrumentation.
 */
private fun resolvePreference(
    a: AndroidInstrumentation,
    b: AndroidInstrumentation,
    aGeneral: Int,
    bGeneral: Int,
    warn: (String) -> Unit,
): Int {
    val ab = a.preferenceOver(b)
    val ba = b.preferenceOver(a)
    if (ab != 0 && ab == ba) {
        // Both claim the same position, so a preference specific to the other one wins.
        val aSpecific = ab != aGeneral
        val bSpecific = ba != bGeneral
        return when {
            aSpecific && !bSpecific -> {
                ab
            }

            bSpecific && !aSpecific -> {
                -ba
            }

            else -> {
                warn("Instrumentations '${a.name}' and '${b.name}' have competing install order preferences")
                0
            }
        }
    }
    return (ab - ba).sign
}

/** An instrumentation no constraint names, used to ask for the preference held about every instrumentation. */
private object UnorderedEmptyAndroidInstrumentation : AndroidInstrumentation {
    override val name: String = ""

    override fun install(
        context: Context,
        openTelemetryRum: OpenTelemetryRum,
    ) {
        // never installed
    }
}

@Suppress("UNCHECKED_CAST")
private fun AndroidInstrumentation.preferenceOver(other: AndroidInstrumentation): Int =
    try {
        (this as? Comparable<AndroidInstrumentation>)?.compareTo(other)?.sign ?: 0
    } catch (_: ClassCastException) {
        0 // implements Comparable of some other type
    }
