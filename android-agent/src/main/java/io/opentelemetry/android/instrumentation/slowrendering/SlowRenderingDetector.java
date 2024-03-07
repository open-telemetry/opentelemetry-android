/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering;

import android.os.Build;
import android.util.Log;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.instrumentation.common.InstrumentedApplication;
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
