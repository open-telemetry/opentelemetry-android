/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.dsl.instrumentation

import io.opentelemetry.android.instrumentation.common.EventAttributesExtractor

internal interface WithEventAttributes<T> {
    /**
     * Supplies an [EventAttributesExtractor] which can be used to customise the attributes on
     * an event.
     */
    fun addAttributesExtractor(value: EventAttributesExtractor<T>)
}
