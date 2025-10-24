/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

internal interface CanBeEnabledAndDisabled {
    /**
     * Controls whether this feature is enabled or not.
     */
    fun enabled(enabled: Boolean)
}
