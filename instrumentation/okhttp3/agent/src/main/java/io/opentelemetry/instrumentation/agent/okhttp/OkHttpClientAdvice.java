/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp;

import io.opentelemetry.instrumentation.library.okhttp.internal.OkHttpSingletons;
import net.bytebuddy.asm.Advice;
import okhttp3.OkHttpClient;

public class OkHttpClientAdvice {

    @Advice.OnMethodEnter
    public static void enter(@Advice.Argument(0) OkHttpClient.Builder builder) {
        if (!builder.interceptors().contains(OkHttpSingletons.CALLBACK_CONTEXT_INTERCEPTOR)) {
            builder.interceptors().add(0, OkHttpSingletons.CALLBACK_CONTEXT_INTERCEPTOR);
            builder.interceptors().add(1, OkHttpSingletons.RESEND_COUNT_CONTEXT_INTERCEPTOR);
            builder.interceptors().add(2, OkHttpSingletons.connectionErrorInterceptor);
        }
        if (!builder.networkInterceptors().contains(OkHttpSingletons.tracingInterceptor)) {
            builder.addNetworkInterceptor(OkHttpSingletons.tracingInterceptor);
        }
    }
}
