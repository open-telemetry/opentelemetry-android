/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.click

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

/**
 * An opentelemetry Modifier that allows to mark a composable element as traceable.
 * When a composable element that has this Modifier will be tapped, then the [name] from this
 * Modifier will be taken as the element name
 *
 * @param name name of the tapped element
 */
fun Modifier.opentelemetry(name: String): Modifier =
    this.semantics {
        this.opentelemetry = name
    }

internal val OpentelemetrySemanticsPropertyKey: SemanticsPropertyKey<String> =
    SemanticsPropertyKey(
        name = "_opentelemetry_semantics",
        mergePolicy = { parentValue, _ ->
            parentValue
        },
    )

private var SemanticsPropertyReceiver.opentelemetry by OpentelemetrySemanticsPropertyKey
