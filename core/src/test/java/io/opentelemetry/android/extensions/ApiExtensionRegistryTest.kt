/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.extensions

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.opentelemetry.android.common.RumConstants.OTEL_RUM_LOG_TAG
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog

@RunWith(AndroidJUnit4::class)
class ApiExtensionRegistryTest {
    @Test
    fun `exact keys and independent snapshots`() {
        val first = TestApiProvider()
        val other = OtherTestApiProvider()
        val registrations = mutableListOf<ApiExtension>(first, other)
        val registry = ApiExtensionRegistry.create(emptyList(), registrations)
        val replacement = TestApiProvider()
        registrations[0] = replacement
        val next = ApiExtensionRegistry.create(emptyList(), registrations)

        assertThat(registry.get(TestApi::class.java)).isSameAs(first)
        assertThat(registry.get(OtherTestApi::class.java)).isSameAs(other)
        assertThat(registry.get(first.javaClass)).isNull()
        assertThat(registry.get(ApiExtension::class.java)).isNull()
        assertThat(next.get(TestApi::class.java)).isSameAs(replacement)
        assertThat(ApiExtensionRegistry.create(emptyList(), emptyList()).get(TestApi::class.java)).isNull()
    }

    @Test
    fun `last provider wins with warning for every replacement source`() {
        val first = TestApiProvider()
        val replacement = object : TestApi {}
        val inputs =
            listOf(
                listOf(first, replacement) to emptyList(),
                listOf(first) to listOf(replacement),
                emptyList<TestApi>() to listOf(first, replacement),
            )
        for ((discovered, registered) in inputs) {
            ShadowLog.clear()
            val registry = ApiExtensionRegistry.create(discovered, registered)
            assertThat(registry.get(TestApi::class.java)).isSameAs(replacement)
            val warning = ShadowLog.getLogsForTag(OTEL_RUM_LOG_TAG).single()
            assertThat(warning.type).isEqualTo(Log.WARN)
            assertThat(warning.msg).contains(TestApi::class.java.name, first.javaClass.name, replacement.javaClass.name)
        }
    }

    @Test
    fun `mismatched providers are skipped with warning`() {
        val invalid = MismatchedApiProvider()
        val inputs =
            listOf(
                listOf(invalid) to emptyList(),
                emptyList<ApiExtension>() to listOf(invalid),
            )
        for ((discovered, registered) in inputs) {
            ShadowLog.clear()
            val registry = ApiExtensionRegistry.create(discovered, registered)
            assertThat(registry.get(TestApi::class.java)).isNull()
            val warning = ShadowLog.getLogsForTag(OTEL_RUM_LOG_TAG).single()
            assertThat(warning.type).isEqualTo(Log.WARN)
            assertThat(warning.msg).contains(TestApi::class.java.name, invalid.javaClass.name, "does not implement")
        }
    }

    @Test
    fun `mismatched providers do not replace valid providers`() {
        val valid = TestApiProvider()
        val invalid = MismatchedApiProvider()
        val inputs =
            listOf(
                listOf(valid, invalid) to emptyList(),
                listOf(valid) to listOf(invalid),
                emptyList<ApiExtension>() to listOf(valid, invalid),
            )
        for ((discovered, registered) in inputs) {
            ShadowLog.clear()
            val registry = ApiExtensionRegistry.create(discovered, registered)
            assertThat(registry.get(TestApi::class.java)).isSameAs(valid)
            assertThat(ShadowLog.getLogsForTag(OTEL_RUM_LOG_TAG)).hasSize(1)
        }
    }

    @Test
    fun `real service metadata is discovered and can be overridden`() {
        val discovered = ApiExtensionLoader.load()
        val provider = discovered.single { it is TestApiProvider }
        val replacement = object : TestApi {}
        assertThat(ApiExtensionRegistry.create(discovered, emptyList()).get(TestApi::class.java)).isSameAs(provider)
        assertThat(ApiExtensionRegistry.create(discovered, listOf(replacement)).get(TestApi::class.java)).isSameAs(replacement)
    }

    private class MismatchedApiProvider : ApiExtension {
        override val type: Class<out ApiExtension> get() = TestApi::class.java
    }
}
