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

    internal var exportScheduleDelay: Long? = null
    internal var autoDetectExportSchedule: Boolean = false

    override fun enabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun exportScheduleDelay(limit: Long) {
        this.exportScheduleDelay = limit
    }

    fun autoDetectExportSchedule(enabled: Boolean) {
        this.autoDetectExportSchedule = enabled
    }
}
