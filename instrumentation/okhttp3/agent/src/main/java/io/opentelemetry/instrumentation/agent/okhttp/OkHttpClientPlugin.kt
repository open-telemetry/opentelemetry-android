/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp

import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import okhttp3.OkHttpClient
import java.io.IOException

internal class OkHttpClientPlugin : Plugin {
    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator,
    ): DynamicType.Builder<*> =
        builder.visit(
            Advice
                .to(OkHttpClientAdvice::class.java)
                .on(
                    ElementMatchers
                        .isConstructor<MethodDescription>()
                        .and(
                            ElementMatchers.takesArguments(
                                OkHttpClient.Builder::class.java,
                            ),
                        ),
                ),
        )

    @Throws(IOException::class)
    override fun close() {
        // No operation.
    }

    override fun matches(target: TypeDescription): Boolean = target.typeName == "okhttp3.OkHttpClient"
}
