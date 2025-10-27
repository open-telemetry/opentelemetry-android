/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.dsl.instrumentation.CanBeEnabledAndDisabled

/**
 * Type-safe config DSL that controls how disk buffering of exported telemetry should behave.
 */
@OpenTelemetryDslMarker
class DiskBufferingConfigurationSpec internal constructor() : CanBeEnabledAndDisabled {
    internal var enabled: Boolean = true

    override fun enabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
