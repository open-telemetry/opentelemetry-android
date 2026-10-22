/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

/**
 * A manual API contributed by an optional module without adding feature-specific members to
 * [io.opentelemetry.android.OpenTelemetryRum].
 *
 * Define a public interface extending [ApiExtension] and keep its implementation internal.
 * Providers can be discovered through `ServiceLoader` (`@AutoService(ApiExtension::class)`) or
 * registered explicitly. Explicit registrations replace discovered providers
 * with the same [type]; the last provider encountered for a key wins, with a warning on replacement.
 *
 * Extensions are resolved before instrumentation installation and snapshotted per RUM instance.
 * They are facades owned by their modules: RUM adds no installation or shutdown lifecycle.
 */
interface ApiExtension {
    /** The exact public interface implemented by this extension and used as its lookup key. */
    val type: Class<out ApiExtension>
}
