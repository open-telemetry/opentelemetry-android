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

package io.opentelemetry.rum.internal.instrumentation.slowrendering;

import static io.opentelemetry.rum.internal.RumConstants.OTEL_RUM_LOG_TAG;

import android.util.Log;

import java.time.Duration;

/**
 * A builder of {@link SlowRenderingDetector}.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class SlowRenderingDetectorBuilder {

    SlowRenderingDetectorBuilder() {}

    Duration slowRenderingDetectionPollInterval = Duration.ofSeconds(1);

    /**
     * Configures the rate at which frame render durations are polled.
     *
     * @param interval The period that should be used for polling.
     * @return {@code this}
     */
    public SlowRenderingDetectorBuilder setSlowRenderingDetectionPollInterval(Duration interval) {
        if (interval.toMillis() <= 0) {
            Log.e(
                    OTEL_RUM_LOG_TAG,
                    "Invalid slowRenderingDetectionPollInterval: "
                            + interval
                            + "; must be positive");
            return this;
        }
        this.slowRenderingDetectionPollInterval = interval;
        return this;
    }

    public SlowRenderingDetector build() {
        return new SlowRenderingDetector(this);
    }
}
