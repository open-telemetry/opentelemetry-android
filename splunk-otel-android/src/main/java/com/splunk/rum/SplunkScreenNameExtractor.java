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

package com.splunk.rum;

import android.app.Activity;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import io.opentelemetry.rum.internal.instrumentation.ScreenNameExtractor;
import java.util.function.Function;

/**
 * Screen name extractor that supports the original Splunk annotation but falls back to
 * OpenTelemetry defaults when not present.
 */
class SplunkScreenNameExtractor implements ScreenNameExtractor {

    static ScreenNameExtractor INSTANCE = new SplunkScreenNameExtractor();

    private SplunkScreenNameExtractor() {}

    @Nullable
    @Override
    public String extract(Activity activity) {
        return getOrDefault(activity, DEFAULT::extract);
    }

    @Nullable
    @Override
    public String extract(Fragment fragment) {
        return getOrDefault(fragment, DEFAULT::extract);
    }

    @Nullable
    private <T> String getOrDefault(T obj, Function<T, String> defaultMethod) {
        RumScreenName rumScreenName = obj.getClass().getAnnotation(RumScreenName.class);
        if (rumScreenName != null) {
            return rumScreenName.value();
        }
        return defaultMethod.apply(obj);
    }
}
