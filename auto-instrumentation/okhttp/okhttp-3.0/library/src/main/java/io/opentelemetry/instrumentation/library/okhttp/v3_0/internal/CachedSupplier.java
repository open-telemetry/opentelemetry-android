/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal;

import java.util.function.Supplier;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class CachedSupplier<T> implements Supplier<T> {
    private Supplier<T> supplier;
    private T instance;
    private final Object lock = new Object();

    public static <T> CachedSupplier<T> create(Supplier<T> instance) {
        return new CachedSupplier<>(instance);
    }

    private CachedSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        synchronized (lock) {
            if (instance == null) {
                instance = supplier.get();
                if (instance == null) {
                    throw new NullPointerException("Supplier provided null.");
                }
                supplier = null;
            }
            return instance;
        }
    }
}
