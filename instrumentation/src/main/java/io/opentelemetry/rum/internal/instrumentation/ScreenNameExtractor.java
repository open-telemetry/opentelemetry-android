/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.rum.internal.instrumentation;

import android.app.Activity;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public interface ScreenNameExtractor {

    @Nullable
    String extract(Activity activity);

    @Nullable
    String extract(Fragment fragment);

    ScreenNameExtractor DEFAULT =
            new ScreenNameExtractor() {
                @Nullable
                @Override
                public String extract(Activity activity) {
                    return useAnnotationOrClassName(activity.getClass());
                }

                @Nullable
                @Override
                public String extract(Fragment fragment) {
                    return useAnnotationOrClassName(fragment.getClass());
                }

                private String useAnnotationOrClassName(Class<?> clazz) {
                    RumScreenName rumScreenName = clazz.getAnnotation(RumScreenName.class);
                    return rumScreenName == null ? clazz.getSimpleName() : rumScreenName.value();
                }
            };
}
