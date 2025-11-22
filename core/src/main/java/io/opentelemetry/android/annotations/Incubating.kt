/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.annotations

/**
 * Marks an API as incubating. Incubating APIs can be subject to breaking change without warning
 * between versions, and require an opt-in annotation to prevent compiler warnings.
 *
 * This annotation is intended for use on opentelemetry-android's API only and should not be used
 * by library consumers on their own code.
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
annotation class Incubating
