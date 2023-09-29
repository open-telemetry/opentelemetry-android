/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.agent.okhttp.v3_0.callback;

import net.bytebuddy.asm.Advice;

import io.opentelemetry.instrumentation.library.okhttp.v3_0.internal.OkHttpCallbackAdviceHelper;
import okhttp3.Call;

public class OkHttpCallbackAdvice {

    @Advice.OnMethodEnter
    public static void enter(@Advice.This Call call) {
        OkHttpCallbackAdviceHelper.onEnterDispatcherEnqueue(call);
    }
}
