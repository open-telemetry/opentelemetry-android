/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.spanannotation

import io.opentelemetry.instrumentation.annotations.SpanAttribute
import io.opentelemetry.instrumentation.annotations.WithSpan

class TestApp {
    @WithSpan("Span-Name")
    fun annotatedMethod(
        @SpanAttribute("attribute") value: String,
    ): String = "Hello $value"
}
