/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
