/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class LazyInterceptor<T extends Interceptor> implements Interceptor {
    private final CachedSupplier<T> interceptorSupplier;

    public LazyInterceptor(CachedSupplier<T> interceptorSupplier) {
        this.interceptorSupplier = interceptorSupplier;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        return interceptorSupplier.get().intercept(chain);
    }
}
