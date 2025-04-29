/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common;

import io.opentelemetry.android.instrumentation.annotations.RumScreenName;

public interface ScreenNameExtractor {

    String extract(Object instance);

    ScreenNameExtractor DEFAULT =
            new ScreenNameExtractor() {
                @Override
                public String extract(Object instance) {
                    return useAnnotationOrClassName(instance.getClass());
                }

                private String useAnnotationOrClassName(Class<?> clazz) {
                    RumScreenName rumScreenName = clazz.getAnnotation(RumScreenName.class);
                    return rumScreenName == null ? clazz.getSimpleName() : rumScreenName.value();
                }
            };
}
