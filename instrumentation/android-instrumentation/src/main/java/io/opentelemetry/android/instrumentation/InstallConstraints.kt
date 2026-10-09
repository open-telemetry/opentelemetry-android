/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation

/**
 * Declares where an [AndroidInstrumentation] should be installed relative to other instrumentations.
 * Intended to be used as a delegate:
 *
 * ```
 * class MyInstrumentation : AndroidInstrumentation,
 *     Comparable<AndroidInstrumentation> by InstallConstraints(installBefore = setOf("native-crash"))
 * ```
 *
 * Constraints that name another instrumentation (by [AndroidInstrumentation.name]) take precedence
 * over [installFirst] and [installLast], so an instrumentation can be installed first while still
 * being installed after a specific other instrumentation. Named instrumentations do not need to
 * declare any constraints of their own.
 *
 * @param installBefore names of instrumentations that should be installed after this one.
 * @param installAfter names of instrumentations that should be installed before this one.
 * @param installFirst install before all instrumentations not named in [installAfter].
 * @param installLast install after all instrumentations not named in [installBefore].
 */
class InstallConstraints(
    private val installBefore: Set<String> = emptySet(),
    private val installAfter: Set<String> = emptySet(),
    private val installFirst: Boolean = false,
    private val installLast: Boolean = false,
) : Comparable<AndroidInstrumentation> {
    init {
        require(!(installFirst && installLast)) {
            "Cannot install both first and last"
        }

        require(installBefore.none { it in installAfter }) {
            "Cannot install both before and after: ${installBefore intersect installAfter}"
        }
    }

    override fun compareTo(other: AndroidInstrumentation): Int =
        when (other.name) {
            in installBefore -> {
                -1
            }

            in installAfter -> {
                1
            }

            else -> {
                when {
                    installFirst -> -1
                    installLast -> 1
                    else -> 0
                }
            }
        }
}
