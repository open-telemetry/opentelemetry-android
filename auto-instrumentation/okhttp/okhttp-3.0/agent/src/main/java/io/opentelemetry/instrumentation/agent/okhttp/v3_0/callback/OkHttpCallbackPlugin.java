/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp.v3_0.callback;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import java.io.IOException;
import java.util.regex.Pattern;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import okhttp3.Callback;

public class OkHttpCallbackPlugin implements Plugin {
    private static final Pattern REAL_CALL_PATTERN = Pattern.compile("^okhttp3\\..*RealCall$");

    @Override
    public DynamicType.Builder<?> apply(
            DynamicType.Builder<?> builder,
            TypeDescription typeDescription,
            ClassFileLocator classFileLocator) {
        return builder.visit(
                Advice.to(OkHttpCallbackAdvice.class)
                        .on(named("enqueue").and(takesArgument(0, Callback.class))));
    }

    @Override
    public void close() throws IOException {
        // No operation.
    }

    @Override
    public boolean matches(TypeDescription target) {
        return REAL_CALL_PATTERN.matcher(target.getTypeName()).matches();
    }
}
