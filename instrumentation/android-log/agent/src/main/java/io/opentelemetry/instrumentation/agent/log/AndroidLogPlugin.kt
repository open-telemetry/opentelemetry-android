/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.log

import android.util.Log
import io.opentelemetry.instrumentation.library.log.AndroidLogSubstitutions
import net.bytebuddy.asm.MemberSubstitution
import net.bytebuddy.build.AndroidDescriptor
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import java.io.IOException

internal class AndroidLogPlugin(
    private val androidDescriptor: AndroidDescriptor,
) : Plugin {
    override fun apply(
        builder: DynamicType.Builder<*>,
        typeDescription: TypeDescription,
        classFileLocator: ClassFileLocator,
    ): DynamicType.Builder<*> {
        try {
            return builder.visit(
                MemberSubstitution
                    .relaxed()
                    .method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "v",
                                String::class.java,
                                String::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForVerbose",
                            String::class.java,
                            String::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "v",
                                String::class.java,
                                String::class.java,
                                Throwable::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForVerbose2",
                            String::class.java,
                            String::class.java,
                            Throwable::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "d",
                                String::class.java,
                                String::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForDebug",
                            String::class.java,
                            String::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "d",
                                String::class.java,
                                String::class.java,
                                Throwable::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForDebug2",
                            String::class.java,
                            String::class.java,
                            Throwable::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "i",
                                String::class.java,
                                String::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForInfo",
                            String::class.java,
                            String::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "i",
                                String::class.java,
                                String::class.java,
                                Throwable::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForInfo2",
                            String::class.java,
                            String::class.java,
                            Throwable::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "w",
                                String::class.java,
                                String::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForWarn",
                            String::class.java,
                            String::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "w",
                                String::class.java,
                                Throwable::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForWarn2",
                            String::class.java,
                            Throwable::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "w",
                                String::class.java,
                                String::class.java,
                                Throwable::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForWarn3",
                            String::class.java,
                            String::class.java,
                            Throwable::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "e",
                                String::class.java,
                                String::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForError",
                            String::class.java,
                            String::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "e",
                                String::class.java,
                                String::class.java,
                                Throwable::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForError2",
                            String::class.java,
                            String::class.java,
                            Throwable::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "wtf",
                                String::class.java,
                                String::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForWtf",
                            String::class.java,
                            String::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "wtf",
                                String::class.java,
                                Throwable::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForWtf2",
                            String::class.java,
                            Throwable::class.java,
                        ),
                    ).method(
                        ElementMatchers.`is`(
                            Log::class.java.getDeclaredMethod(
                                "wtf",
                                String::class.java,
                                String::class.java,
                                Throwable::class.java,
                            ),
                        ),
                    ).replaceWith(
                        AndroidLogSubstitutions::class.java.getDeclaredMethod(
                            "substitutionForWtf3",
                            String::class.java,
                            String::class.java,
                            Throwable::class.java,
                        ),
                    ).on(ElementMatchers.isMethod()),
            )
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        // No operation.
    }

    override fun matches(target: TypeDescription): Boolean = androidDescriptor.getTypeScope(target) == AndroidDescriptor.TypeScope.LOCAL
}
