/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.export

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.data.DelegatingSpanData
import io.opentelemetry.sdk.trace.data.SpanData

internal class ModifiedSpanData(
    original: SpanData,
    private val modifiedAttributes: Attributes,
) : DelegatingSpanData(original) {
    override fun getAttributes(): Attributes = modifiedAttributes

    override fun getTotalAttributeCount(): Int = modifiedAttributes.size()
}
