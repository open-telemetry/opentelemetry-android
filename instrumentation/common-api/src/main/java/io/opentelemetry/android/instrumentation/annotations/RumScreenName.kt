/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.annotations

/**
 * This annotation can be used to customize the `screen.name` attribute for an instrumented
 * Fragment or Activity.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class RumScreenName(
    /** Return the customized screen name.  */
    val value: String,
)
