/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp.callback;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.library.okhttp.internal.OkHttpCallbackAdviceHelper;
import io.opentelemetry.instrumentation.library.okhttp.internal.TracingCallback;
import net.bytebuddy.asm.Advice;
import okhttp3.Call;
import okhttp3.Callback;

public class OkHttpCallbackAdvice {

    @Advice.OnMethodEnter
    public static void enter(
            @Advice.This Call call,
            @Advice.Argument(value = 0, readOnly = false) Callback callback) {
        if (OkHttpCallbackAdviceHelper.propagateContext(call)) {
            callback = new TracingCallback(callback, Context.current());
        }
    }
}
