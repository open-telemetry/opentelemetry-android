/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.screenorientation.model

import android.content.res.Configuration

/**
 * Representing a screen orientation value.
 *
 * @property value The integer orientation constant.
 * May be one of [Configuration.ORIENTATION_LANDSCAPE] or [Configuration.ORIENTATION_PORTRAIT] .
 *
 * @see Configuration.orientation
 */
@JvmInline
value class Orientation(
    val value: Int,
)
