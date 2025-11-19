/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.compose.click

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics

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
