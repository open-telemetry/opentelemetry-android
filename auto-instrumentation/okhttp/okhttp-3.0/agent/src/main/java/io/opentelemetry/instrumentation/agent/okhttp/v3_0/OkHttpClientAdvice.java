/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp.v3_0;

import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttp3Singletons;

import net.bytebuddy.asm.Advice;

import okhttp3.OkHttpClient;

public class OkHttpClientAdvice {

    @Advice.OnMethodEnter
    public static void enter(@Advice.Argument(0) OkHttpClient.Builder builder) {
        if (!builder.interceptors().contains(OkHttp3Singletons.CONTEXT_INTERCEPTOR)) {
            builder.interceptors().add(0, OkHttp3Singletons.CONTEXT_INTERCEPTOR);
            builder.interceptors().add(1, OkHttp3Singletons.CONNECTION_ERROR_INTERCEPTOR);
            builder.interceptors().add(2, OkHttp3Singletons.TRACING_INTERCEPTOR);
        }
    }
}
