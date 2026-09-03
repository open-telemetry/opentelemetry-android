/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

/**
 * A manual API that optional modules make available through
 * [io.opentelemetry.android.OpenTelemetryRum.getExtension].
 *
 * [io.opentelemetry.android.OpenTelemetryRum] only exposes what every setup needs. Some modules
 * need to let users provide information that cannot be reliably inferred on their behalf, such as
 * whether the user is currently active, or what the current screen is. Those modules define an
 * [ApiExtension] instead of adding members to [io.opentelemetry.android.OpenTelemetryRum].
 *
 * Conventions:
 * - The public contract is an interface that extends [ApiExtension] and provides [type].
 *   Implementations stay internal to the module that owns them.
 * - An extension is a facade over the internals of the module that owns it. It has no lifecycle.
 * - The extension is the single source of truth for what it controls. Automatic instrumentations
 *   that infer the same information write through the extension too, so the last write wins.
 * - Modules may expose a Kotlin extension function on [io.opentelemetry.android.OpenTelemetryRum]
 *   for convenience, e.g. `fun OpenTelemetryRum.sessions(): SessionActivityApi?`.
 *
 * Extensions become available to an [io.opentelemetry.android.OpenTelemetryRum] instance in two
 * ways:
 * - Discovered on the classpath via `ServiceLoader` (`@AutoService(ApiExtension::class)`), for
 *   extensions that don't need anything created during initialization.
 * - Registered explicitly through the RUM builder, for extensions that wrap internals created
 *   during initialization.
 *
 * Extensions are resolved before instrumentations are installed, so an
 * [io.opentelemetry.android.instrumentation.AndroidInstrumentation] can obtain them from the
 * [io.opentelemetry.android.OpenTelemetryRum] it receives in `install()`.
 */
interface ApiExtension {
    /**
     * The public type this extension is registered under. Lookups through
     * [io.opentelemetry.android.OpenTelemetryRum.getExtension] must use this exact type.
     */
    val type: Class<out ApiExtension>
}
