/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp.callback

import java.io.IOException
import java.util.regex.Pattern
import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import okhttp3.Callback

internal class OkHttpCallbackPlugin : Plugin {

    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator
    ): DynamicType.Builder<*> {
        return builder.visit(
            Advice.to(OkHttpCallbackAdvice::class.java)
                .on(
                    ElementMatchers.named<NamedElement>("enqueue").and(
                        ElementMatchers.takesArgument(
                            0,
                            Callback::class.java
                        )
                    )
                )
        )
    }

    @Throws(IOException::class)
    override fun close() {
        // No operation.
    }

    override fun matches(target: TypeDescription): Boolean {
        return REAL_CALL_PATTERN.matcher(target.typeName).matches()
    }

    companion object {
        private val REAL_CALL_PATTERN: Pattern = Pattern.compile("^okhttp3\\..*RealCall$")
    }
}
