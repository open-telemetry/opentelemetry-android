package io.opentelemetry.instrumentation.library.okhttp.v3_0

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SimpleTest {

    @Test
    fun name() {
        assertThat("1").isNotEqualTo(1)
    }
}