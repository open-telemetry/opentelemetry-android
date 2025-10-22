/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

internal interface CanBeEnabledAndDisabled {
    fun enabled(enabled: Boolean)
}
