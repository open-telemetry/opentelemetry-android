/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp.v3_0;

import java.io.IOException;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import okhttp3.OkHttpClient;

public class OkHttpClientPlugin implements Plugin {

    @Override
    public DynamicType.Builder<?> apply(
            DynamicType.Builder<?> builder,
            TypeDescription typeDescription,
            ClassFileLocator classFileLocator) {
        return builder.visit(
                Advice.to(OkHttpClientAdvice.class)
                        .on(
                                ElementMatchers.isConstructor()
                                        .and(
                                                ElementMatchers.takesArguments(
                                                        OkHttpClient.Builder.class))));
    }

    @Override
    public void close() throws IOException {
        // No operation.
    }

    @Override
    public boolean matches(TypeDescription target) {
        return target.getTypeName().equals("okhttp3.OkHttpClient");
    }
}
