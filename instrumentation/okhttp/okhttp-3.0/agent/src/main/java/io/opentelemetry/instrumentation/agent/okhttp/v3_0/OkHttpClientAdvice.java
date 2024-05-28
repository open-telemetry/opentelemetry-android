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
        if (!builder.interceptors().contains(OkHttp3Singletons.CALLBACK_CONTEXT_INTERCEPTOR)) {
            builder.interceptors().add(0, OkHttp3Singletons.CALLBACK_CONTEXT_INTERCEPTOR);
            builder.interceptors().add(1, OkHttp3Singletons.RESEND_COUNT_CONTEXT_INTERCEPTOR);
            builder.interceptors().add(2, OkHttp3Singletons.CONNECTION_ERROR_INTERCEPTOR);
        }
        if (!builder.networkInterceptors().contains(OkHttp3Singletons.TRACING_INTERCEPTOR)) {
            builder.addNetworkInterceptor(OkHttp3Singletons.TRACING_INTERCEPTOR);
        }
    }
}
