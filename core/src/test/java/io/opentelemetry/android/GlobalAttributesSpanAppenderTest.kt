/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.function.Supplier

internal class GlobalAttributesSpanAppenderTest {
    @MockK
    lateinit var span: ReadWriteSpan

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { span.setAllAttributes(any()) } returns span
    }

    @Test
    fun shouldAppendGlobalAttributes() {
        val globalAttributes =
            GlobalAttributesSpanAppender {
                Attributes.of(AttributeKey.stringKey("key"), "value")
            }
        globalAttributes.update { attributesBuilder: AttributesBuilder ->
            attributesBuilder.put(
                "key",
                "value2",
            )
        }
        globalAttributes.update { attributesBuilder: AttributesBuilder ->
            attributesBuilder.put(
                AttributeKey.longKey("otherKey"),
                1234L,
            )
        }

        assertThat(globalAttributes.isStartRequired).isTrue()
        globalAttributes.onStart(Context.root(), span)

        verify {
            span.setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("key"),
                    "value2",
                    AttributeKey.longKey("otherKey"),
                    1234L,
                ),
            )
        }

        assertThat(globalAttributes.isEndRequired).isFalse()
    }

    @Test
    fun createWithSupplier() {
        val attrs = Attributes.of(AttributeKey.stringKey("foo"), "bar")
        val globalAttributes = GlobalAttributesSpanAppender { attrs }

        globalAttributes.onStart(Context.root(), span)
        verify {
            span.setAllAttributes(Attributes.of(AttributeKey.stringKey("foo"), "bar"))
        }
    }

    @Test
    fun updateWithSupplierReplacesSupplier() {
        val attrs = Attributes.of(AttributeKey.stringKey("foo"), "bar")
        val originalSupplier =
            Supplier { fail<Attributes>("Should not have been called") }

        val globalAttributes = GlobalAttributesSpanAppender(originalSupplier)
        globalAttributes.update(Supplier { attrs })

        globalAttributes.onStart(Context.root(), span)
        verify {
            span.setAllAttributes(Attributes.of(AttributeKey.stringKey("foo"), "bar"))
        }
    }

    @Test
    fun updateWithAttributesReplacesSupplier() {
        val attrs = Attributes.of(AttributeKey.stringKey("foo"), "bar")
        val extra = Attributes.of(AttributeKey.stringKey("bar"), "baz")
        val originalSupplier = mockk<Supplier<Attributes>>()

        every { originalSupplier.get() } returns attrs andThenThrows RuntimeException("Should not have been called again.")

        val globalAttributes = GlobalAttributesSpanAppender(originalSupplier)
        globalAttributes.update { builder: AttributesBuilder ->
            builder.putAll(
                extra,
            )
        }

        globalAttributes.onStart(Context.root(), span)
        verify {
            span.setAllAttributes(
                Attributes.of(
                    AttributeKey.stringKey("foo"),
                    "bar",
                    AttributeKey.stringKey("bar"),
                    "baz",
                ),
            )
        }
    }
}
