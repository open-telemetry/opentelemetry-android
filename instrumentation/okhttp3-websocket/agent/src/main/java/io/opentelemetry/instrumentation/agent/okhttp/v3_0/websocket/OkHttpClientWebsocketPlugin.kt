/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp.v3_0.websocket

import java.io.IOException
import net.bytebuddy.asm.Advice
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.NamedElement
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import okhttp3.WebSocketListener

internal class OkHttpClientWebsocketPlugin : Plugin {

    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator
    ): DynamicType.Builder<*> {
        return builder.visit(
            Advice.to(OkHttpClientWebsocketAdvice::class.java)
                .on(
                    ElementMatchers.named<NamedElement>("newWebSocket").and(
                        ElementMatchers.takesArgument(
                            1,
                            WebSocketListener::class.java
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
        return target.typeName == "okhttp3.OkHttpClient"
    }
}
