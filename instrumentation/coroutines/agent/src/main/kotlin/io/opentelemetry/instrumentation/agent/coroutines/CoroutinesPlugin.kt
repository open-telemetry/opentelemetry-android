/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.coroutines

import io.opentelemetry.instrumentation.library.coroutines.internal.CoroutinesLaunchBridge
import net.bytebuddy.asm.MemberSubstitution
import net.bytebuddy.build.AndroidDescriptor
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers

internal class CoroutinesPlugin(
    private val androidDescriptor: AndroidDescriptor,
) : Plugin {
    override fun matches(target: TypeDescription): Boolean = androidDescriptor.getTypeScope(target) == AndroidDescriptor.TypeScope.LOCAL

    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator,
    ): DynamicType.Builder<*> =
        builder.visit(
            MemberSubstitution
                .relaxed()
                .method(
                    ElementMatchers
                        .named<MethodDescription>("launch")
                        .and(ElementMatchers.isDeclaredBy(ElementMatchers.named("kotlinx.coroutines.BuildersKt"))),
                ).replaceWith(BRIDGE_LAUNCH)
                .method(
                    ElementMatchers
                        .named<MethodDescription>("launch\$default")
                        .and(ElementMatchers.isDeclaredBy(ElementMatchers.named("kotlinx.coroutines.BuildersKt"))),
                ).replaceWith(BRIDGE_LAUNCH_DEFAULT)
                .on(ElementMatchers.any()),
        )

    override fun close() {}

    private companion object {
        private val BRIDGE_TYPE = TypeDescription.ForLoadedType.of(CoroutinesLaunchBridge::class.java)
        private val BRIDGE_LAUNCH: MethodDescription =
            BRIDGE_TYPE.declaredMethods
                .filter(
                    ElementMatchers
                        .named<MethodDescription>("launch")
                        .and(ElementMatchers.isStatic())
                        .and(ElementMatchers.takesArguments(4)),
                ).getOnly()
        private val BRIDGE_LAUNCH_DEFAULT: MethodDescription =
            BRIDGE_TYPE.declaredMethods
                .filter(
                    ElementMatchers
                        .named<MethodDescription>("launch\$default")
                        .and(ElementMatchers.isStatic()),
                ).getOnly()
    }
}
