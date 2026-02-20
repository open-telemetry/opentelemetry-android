/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.httpurlconnection

import io.opentelemetry.instrumentation.library.httpurlconnection.HttpUrlReplacements
import net.bytebuddy.asm.MemberSubstitution
import net.bytebuddy.build.AndroidDescriptor
import net.bytebuddy.build.Plugin
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.matcher.ElementMatchers.`is`
import java.lang.Long
import java.net.HttpURLConnection
import java.net.URLConnection
import kotlin.Boolean
import kotlin.RuntimeException
import kotlin.String
import kotlin.arrayOf

internal class HttpUrlConnectionPlugin(
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
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "connect",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForConnect",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getContent",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForContent",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getContent",
                                arrayOf<Class<*>>()::class.java,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForContent",
                            URLConnection::class.java,
                            arrayOf<Class<*>>()::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getContentType",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForContentType",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getContentEncoding",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForContentEncoding",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getContentLength",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForContentLength",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getContentLengthLong",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForContentLengthLong",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getExpiration",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForExpiration",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getDate",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForDate",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getLastModified",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForLastModified",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getHeaderField",
                                String::class.java,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHeaderField",
                            URLConnection::class.java,
                            String::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getHeaderFields",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHeaderFields",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getHeaderFieldInt",
                                String::class.java,
                                Integer.TYPE,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHeaderFieldInt",
                            URLConnection::class.java,
                            String::class.java,
                            Integer.TYPE,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getHeaderFieldLong",
                                String::class.java,
                                Long.TYPE,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHeaderFieldLong",
                            URLConnection::class.java,
                            String::class.java,
                            Long.TYPE,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getHeaderField",
                                Integer.TYPE,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHeaderField",
                            URLConnection::class.java,
                            Integer.TYPE,
                        ),
                    ).method(
                        `is`(
                            HttpURLConnection::class.java.getDeclaredMethod(
                                "getHeaderField",
                                Integer.TYPE,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHttpHeaderField",
                            HttpURLConnection::class.java,
                            Integer.TYPE,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getHeaderFieldKey",
                                Integer.TYPE,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHeaderFieldKey",
                            URLConnection::class.java,
                            Integer.TYPE,
                        ),
                    ).method(
                        `is`(
                            HttpURLConnection::class.java.getDeclaredMethod(
                                "getHeaderFieldKey",
                                Integer.TYPE,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHttpHeaderFieldKey",
                            HttpURLConnection::class.java,
                            Integer.TYPE,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getHeaderFieldDate",
                                String::class.java,
                                Long.TYPE,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHeaderFieldDate",
                            URLConnection::class.java,
                            String::class.java,
                            Long.TYPE,
                        ),
                    ).method(
                        `is`(
                            HttpURLConnection::class.java.getDeclaredMethod(
                                "getHeaderFieldDate",
                                String::class.java,
                                Long.TYPE,
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForHttpHeaderFieldDate",
                            HttpURLConnection::class.java,
                            String::class.java,
                            Long.TYPE,
                        ),
                    ).method(
                        `is`(
                            HttpURLConnection::class.java.getDeclaredMethod(
                                "getResponseCode",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForResponseCode",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            HttpURLConnection::class.java.getDeclaredMethod(
                                "getResponseMessage",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForResponseMessage",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getOutputStream",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForOutputStream",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            URLConnection::class.java.getDeclaredMethod(
                                "getInputStream",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForInputStream",
                            URLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            HttpURLConnection::class.java.getDeclaredMethod(
                                "getErrorStream",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForErrorStream",
                            HttpURLConnection::class.java,
                        ),
                    ).method(
                        `is`(
                            HttpURLConnection::class.java.getDeclaredMethod(
                                "disconnect",
                            ),
                        ),
                    ).replaceWith(
                        HttpUrlReplacements::class.java.getDeclaredMethod(
                            "replacementForDisconnect",
                            HttpURLConnection::class.java,
                        ),
                    ).on(ElementMatchers.isMethod()),
            )
        } catch (e: NoSuchMethodException) {
            throw RuntimeException(e)
        }
    }

    override fun close() {
        // No operation.
    }

    override fun matches(target: TypeDescription): Boolean = androidDescriptor.getTypeScope(target) != AndroidDescriptor.TypeScope.EXTERNAL
}
