/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalApi::class)

package io.opentelemetry.android

import io.opentelemetry.kotlin.ExperimentalApi
import io.opentelemetry.kotlin.OpenTelemetry
import io.opentelemetry.kotlin.toOtelKotlinApi
import java.util.WeakHashMap

private val compatLock = Any()

private val compatInstances = WeakHashMap<OpenTelemetryRum, OpenTelemetry>()

/**
 * The opentelemetry-kotlin [OpenTelemetry] API for this instance.
 *
 * This returns the *compat* implementation of opentelemetry-kotlin: a Kotlin API whose calls are
 * delegated under the hood to the OpenTelemetry Java SDK that this agent is built on. It is not a
 * separate SDK, so telemetry recorded through it is processed and exported by exactly the same
 * pipeline as telemetry recorded through [OpenTelemetryRum.openTelemetry], and both APIs may be
 * used side by side.
 *
 * The same instance is returned for repeated accesses on the same [OpenTelemetryRum].
 *
 * This property, and the whole `agent-api-ktx` module, are **not stable**: the module is published
 * with an `-alpha` version suffix. opentelemetry-kotlin is itself pre-1.0 and every type reachable
 * from [OpenTelemetry] is marked [ExperimentalApi], so both this property and the API it exposes
 * may change or be removed in any release, with no deprecation cycle. Opt in with
 * `@OptIn(ExperimentalApi::class)` to acknowledge that.
 */
@ExperimentalApi
val OpenTelemetryRum.openTelemetryKotlin: OpenTelemetry
    get() =
        synchronized(compatLock) {
            compatInstances.getOrPut(this) { openTelemetry.toOtelKotlinApi() }
        }
