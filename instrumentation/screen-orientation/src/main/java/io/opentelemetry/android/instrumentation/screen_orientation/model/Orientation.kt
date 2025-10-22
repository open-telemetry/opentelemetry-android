package io.opentelemetry.android.instrumentation.screen_orientation.model

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
value class Orientation(val value: Int)
