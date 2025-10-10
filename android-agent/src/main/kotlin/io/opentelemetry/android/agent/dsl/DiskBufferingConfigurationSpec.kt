/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl

import io.opentelemetry.android.agent.dsl.instrumentation.CanBeEnabledAndDisabled

@OpenTelemetryDslMarker
class DiskBufferingConfigurationSpec internal constructor() : CanBeEnabledAndDisabled {
    internal var enabled: Boolean = true

    override fun enabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
