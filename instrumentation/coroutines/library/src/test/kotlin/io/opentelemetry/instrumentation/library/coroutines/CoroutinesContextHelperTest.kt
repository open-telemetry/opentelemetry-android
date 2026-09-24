/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.coroutines

import io.mockk.mockk
import io.opentelemetry.android.OpenTelemetryRum
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.extension.kotlin.getOpenTelemetryContext
import io.opentelemetry.instrumentation.library.coroutines.internal.CoroutinesContextHelper
import io.opentelemetry.sdk.testing.junit5.OpenTelemetryExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.coroutines.EmptyCoroutineContext
import android.content.Context as AndroidContext

class CoroutinesContextHelperTest {
    companion object {
        @RegisterExtension
        @JvmField
        val otelExtension: OpenTelemetryExtension = OpenTelemetryExtension.create()
    }

    @BeforeEach
    fun enable() {
        CoroutinesContextHelper.setEnabled(true)
    }

    @AfterEach
    fun disable() {
        CoroutinesContextHelper.setEnabled(false)
    }

    @Test
    fun `disabled instrumentation returns original coroutine context`() {
        CoroutinesContextHelper.setEnabled(false)
        val span =
            otelExtension.openTelemetry
                .getTracer("test")
                .spanBuilder("s")
                .startSpan()
        span.makeCurrent().use {
            val result = CoroutinesContextHelper.addCurrentContextIfNeeded(EmptyCoroutineContext)
            assertThat(result).isSameAs(EmptyCoroutineContext)
        }
        span.end()
    }

    @Test
    fun `root current context does not add element`() {
        val result = CoroutinesContextHelper.addCurrentContextIfNeeded(EmptyCoroutineContext)
        assertThat(result).isSameAs(EmptyCoroutineContext)
    }

    @Test
    fun `non-root current context is added`() {
        val span =
            otelExtension.openTelemetry
                .getTracer("test")
                .spanBuilder("s")
                .startSpan()
        span.makeCurrent().use {
            val result = CoroutinesContextHelper.addCurrentContextIfNeeded(EmptyCoroutineContext)
            assertThat(result).isNotSameAs(EmptyCoroutineContext)
            val otelCtx = result.getOpenTelemetryContext()
            assertThat(otelCtx).isNotEqualTo(Context.root())
        }
        span.end()
    }

    @Test
    fun `existing non-root OTel coroutine context is preserved`() {
        val span1 =
            otelExtension.openTelemetry
                .getTracer("test")
                .spanBuilder("s1")
                .startSpan()
        val existingElement = span1.makeCurrent().use { Context.current().asContextElement() }

        val span2 =
            otelExtension.openTelemetry
                .getTracer("test")
                .spanBuilder("s2")
                .startSpan()
        span2.makeCurrent().use {
            val result = CoroutinesContextHelper.addCurrentContextIfNeeded(existingElement)
            assertThat(result).isSameAs(existingElement)
        }
        span1.end()
        span2.end()
    }

    @Test
    fun `already-present current context is not duplicated`() {
        val span =
            otelExtension.openTelemetry
                .getTracer("test")
                .spanBuilder("s")
                .startSpan()
        span.makeCurrent().use {
            val element = Context.current().asContextElement()
            val result = CoroutinesContextHelper.addCurrentContextIfNeeded(element)
            assertThat(result).isSameAs(element)
        }
        span.end()
    }

    @Test
    fun `install enables and uninstall disables context injection`() {
        val instrumentation = CoroutinesInstrumentation()
        val androidContext = mockk<AndroidContext>()
        val openTelemetryRum = mockk<OpenTelemetryRum>()
        val span =
            otelExtension.openTelemetry
                .getTracer("test")
                .spanBuilder("s")
                .startSpan()
        span.makeCurrent().use {
            instrumentation.uninstall(androidContext, openTelemetryRum)
            val resultDisabled = CoroutinesContextHelper.addCurrentContextIfNeeded(EmptyCoroutineContext)
            assertThat(resultDisabled).isSameAs(EmptyCoroutineContext)

            instrumentation.install(androidContext, openTelemetryRum)
            val resultEnabled = CoroutinesContextHelper.addCurrentContextIfNeeded(EmptyCoroutineContext)
            assertThat(resultEnabled).isNotSameAs(EmptyCoroutineContext)

            instrumentation.uninstall(androidContext, openTelemetryRum)
            val resultUninstalled = CoroutinesContextHelper.addCurrentContextIfNeeded(EmptyCoroutineContext)
            assertThat(resultUninstalled).isSameAs(EmptyCoroutineContext)
        }
        span.end()
    }

    @Test
    fun `context does not leak to caller thread after coroutine completes`() {
        val span =
            otelExtension.openTelemetry
                .getTracer("test")
                .spanBuilder("s")
                .startSpan()
        var contextInsideCoroutine: Context? = null

        runBlocking {
            span.makeCurrent().use {
                val enriched = CoroutinesContextHelper.addCurrentContextIfNeeded(EmptyCoroutineContext)
                val job =
                    launch(enriched + Dispatchers.Default) {
                        delay(10)
                        contextInsideCoroutine = Context.current()
                    }
                job.join()
            }
            assertThat(Context.current()).isEqualTo(Context.root())
        }

        assertThat(contextInsideCoroutine).isNotNull()
        assertThat(contextInsideCoroutine).isNotEqualTo(Context.root())
        span.end()
    }
}
