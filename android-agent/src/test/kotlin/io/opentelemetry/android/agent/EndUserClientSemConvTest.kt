/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent

import io.opentelemetry.android.semconv.AppAttributes
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EndUserClientSemConvTest {
    @Test
    fun `constants generated from the end-user-client registry are usable`() {
        val attributes = setOf(AppAttributes.APP_NAV_DESTINATION, AppAttributes.APP_ENVIRONMENT)
        assertThat(attributes.size).isEqualTo(2)
    }
}
