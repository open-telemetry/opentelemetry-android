/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class TracingCallback implements Callback {
    private final Callback delegate;
    private final Context callingContext;

    public TracingCallback(Callback delegate, Context callingContext) {
        this.delegate = delegate;
        this.callingContext = callingContext;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        try (Scope scope = callingContext.makeCurrent()) {
            delegate.onFailure(call, e);
        }
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        try (Scope scope = callingContext.makeCurrent()) {
            delegate.onResponse(call, response);
        }
    }
}
