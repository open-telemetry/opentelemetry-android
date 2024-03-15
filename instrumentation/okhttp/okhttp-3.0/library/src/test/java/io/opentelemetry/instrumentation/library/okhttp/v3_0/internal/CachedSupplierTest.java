/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.library.okhttp.v3_0.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class CachedSupplierTest {

    @Test
    void provideAndCacheSuppliersResult() {
        Supplier<String> original =
                spy(
                        new Supplier<String>() {
                            @Override
                            public String get() {
                                return "Hello World";
                            }
                        });
        CachedSupplier<String> cached = CachedSupplier.create(original);

        for (int i = 0; i < 3; i++) {
            assertThat(cached.get()).isEqualTo("Hello World");
        }
        verify(original).get();
    }

    @Test
    void validateSupplierDoesNotReturnNull() {
        Supplier<String> original = () -> null;
        CachedSupplier<String> cached = CachedSupplier.create(original);

        try {
            cached.get();
            fail();
        } catch (NullPointerException ignored) {
        }
    }
}
