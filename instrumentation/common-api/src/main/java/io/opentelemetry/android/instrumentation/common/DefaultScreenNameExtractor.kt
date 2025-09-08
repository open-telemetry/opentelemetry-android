/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common

import io.opentelemetry.android.instrumentation.annotations.RumScreenName

object DefaultScreenNameExtractor : ScreenNameExtractor {
    override fun extract(instance: Any): String {
        val rumScreenName = instance.javaClass.getAnnotation(RumScreenName::class.java)
        return rumScreenName?.value ?: instance.javaClass.simpleName
    }
}
