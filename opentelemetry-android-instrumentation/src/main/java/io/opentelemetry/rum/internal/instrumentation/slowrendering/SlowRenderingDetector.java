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

import android.os.Build;
import android.util.Log;
import io.opentelemetry.rum.internal.RumConstants;
import io.opentelemetry.rum.internal.instrumentation.InstrumentedApplication;
import java.time.Duration;

/**
 * Entrypoint for installing the slow rendering detection instrumentation.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class SlowRenderingDetector {

    public static SlowRenderingDetector create() {
        return builder().build();
    }

    public static SlowRenderingDetectorBuilder builder() {
        return new SlowRenderingDetectorBuilder();
    }

    private final Duration slowRenderingDetectionPollInterval;

    SlowRenderingDetector(SlowRenderingDetectorBuilder builder) {
        this.slowRenderingDetectionPollInterval = builder.slowRenderingDetectionPollInterval;
    }

    /**
     * Installs the slow rendering detection instrumentation on the given {@link
     * InstrumentedApplication}.
     */
    public void installOn(InstrumentedApplication instrumentedApplication) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "Slow/frozen rendering detection is not supported on platforms older than Android N (SDK version 24).");
            return;
        }

        SlowRenderListener detector =
                new SlowRenderListener(
                        instrumentedApplication
                                .getOpenTelemetrySdk()
                                .getTracer("io.opentelemetry.slow-rendering"),
                        slowRenderingDetectionPollInterval);

        instrumentedApplication.getApplication().registerActivityLifecycleCallbacks(detector);
        detector.start();
    }
}
