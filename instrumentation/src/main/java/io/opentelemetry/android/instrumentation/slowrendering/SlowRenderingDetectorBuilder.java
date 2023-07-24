/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering;

import android.util.Log;
import io.opentelemetry.android.RumConstants;
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
                    RumConstants.OTEL_RUM_LOG_TAG,
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
