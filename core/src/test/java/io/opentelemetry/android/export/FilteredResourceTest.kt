package io.opentelemetry.android.export

import io.opentelemetry.api.common.AttributeKey.booleanArrayKey
import io.opentelemetry.api.common.AttributeKey.booleanKey
import io.opentelemetry.api.common.AttributeKey.doubleArrayKey
import io.opentelemetry.api.common.AttributeKey.doubleKey
import io.opentelemetry.api.common.AttributeKey.longArrayKey
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.common.AttributeKey.stringArrayKey
import io.opentelemetry.api.common.AttributeKey.stringKey
import io.opentelemetry.sdk.resources.Resource
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FilteredResourceTest {

    @Test
    fun `test filter`(){
        val original = Resource.builder()
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
        val omitted = listOf("skipme1", "skipme2")
        val filteredResource = FilteredResource(original, omitted)
        val result = filteredResource.get()
        assertThat(result.getAttribute(stringKey("string"))).isEqualTo("string");
        assertThat(result.getAttribute(longKey("long"))).isEqualTo(21L);
        assertThat(result.getAttribute(doubleKey("double"))).isEqualTo(21.111);
        assertThat(result.getAttribute(booleanKey("boolean"))).isTrue();
        assertThat(result.getAttribute(stringArrayKey("string.array"))).isEqualTo(listOf("foo", "bar"));
        assertThat(result.getAttribute(longArrayKey("long.array"))).isEqualTo(listOf(67L, 68L, 69L));
        assertThat(result.getAttribute(doubleArrayKey("double.array"))).isEqualTo(listOf(1.1, 2.2, 3.3));
        assertThat(result.getAttribute(booleanArrayKey("bool.array"))).isEqualTo(listOf(true, false, true));
        assertThat(result.getAttribute(stringKey("skipme1"))).isNull()
        assertThat(result.getAttribute(booleanKey("skipme2"))).isNull()
    }

}