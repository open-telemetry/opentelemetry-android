/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.agent.metrics

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.resources.Resource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FilteredResourceTest {
    @Test
    fun `test filter`() {
        val original =
            Resource
                .builder()
                .setSchemaUrl("http://foo.bar.com")
                .put("string", "string")
                .put("skipme1", "xxx")
                .put("long", 21L)
                .put("double", 21.111)
                .put("boolean", true)
                .put("skipme2", true)
                .put("string.array", "foo", "bar")
                .put("long.array", 67, 68, 69)
                .put("double.array", 1.1, 2.2, 3.3)
                .put("bool.array", true, false, true)
                .build()
        val wantKeys = setOf("string", "long", "double", "boolean", "string.array", "long.array", "double.array", "bool.array")
        val filteredResource = FilteredResource(original, wantKeys)
        val result = filteredResource.get()
        assertThat(result.getAttribute(AttributeKey.stringKey("string"))).isEqualTo("string")
        assertThat(result.getAttribute(AttributeKey.longKey("long"))).isEqualTo(21L)
        assertThat(result.getAttribute(AttributeKey.doubleKey("double"))).isEqualTo(21.111)
        assertThat(result.getAttribute(AttributeKey.booleanKey("boolean"))).isTrue()
        assertThat(result.getAttribute(AttributeKey.stringArrayKey("string.array"))).isEqualTo(listOf("foo", "bar"))
        assertThat(result.getAttribute(AttributeKey.longArrayKey("long.array"))).isEqualTo(listOf(67L, 68L, 69L))
        assertThat(result.getAttribute(AttributeKey.doubleArrayKey("double.array"))).isEqualTo(listOf(1.1, 2.2, 3.3))
        assertThat(result.getAttribute(AttributeKey.booleanArrayKey("bool.array"))).isEqualTo(listOf(true, false, true))
        assertThat(result.getAttribute(AttributeKey.stringKey("skipme1"))).isNull()
        assertThat(result.getAttribute(AttributeKey.booleanKey("skipme2"))).isNull()
    }
}
