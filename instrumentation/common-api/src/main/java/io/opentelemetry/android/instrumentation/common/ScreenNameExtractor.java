/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.common;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import io.opentelemetry.android.instrumentation.annotations.RumScreenName;

public interface ScreenNameExtractor {

    String extract(Activity activity);

    String extract(Fragment fragment);

    ScreenNameExtractor DEFAULT =
            new ScreenNameExtractor() {
                @Override
                public String extract(Activity activity) {
                    return useAnnotationOrClassName(activity.getClass());
                }

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
