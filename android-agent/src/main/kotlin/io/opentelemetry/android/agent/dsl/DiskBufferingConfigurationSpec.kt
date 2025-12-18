/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.dsl.instrumentation.CanBeEnabledAndDisabled
import io.opentelemetry.android.config.OtelRumConfig
import io.opentelemetry.android.features.diskbuffering.DiskBufferingConfig

/**
 * Type-safe config DSL that controls how disk buffering of exported telemetry should behave.
 */
@OpenTelemetryDslMarker
class DiskBufferingConfigurationSpec internal constructor(
    private val rumConfig: OtelRumConfig,
) : CanBeEnabledAndDisabled {
    internal var enabled: Boolean = true
        set(value) {
            field = value
            rumConfig.setDiskBufferingConfig(DiskBufferingConfig.create(enabled = value))
        }

    init {
        rumConfig.setDiskBufferingConfig(DiskBufferingConfig.create(enabled))
    }

    override fun enabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
