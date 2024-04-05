/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering;

import android.app.Application;
import android.os.Build;
import android.util.Log;
import io.opentelemetry.android.OpenTelemetryRum;
import io.opentelemetry.android.common.RumConstants;
import java.time.Duration;

/**
 * Entrypoint for installing the slow rendering detection instrumentation.
 *
 * <p>This class is internal and not for public use. Its APIs are unstable and can change at any
 * time.
 */
public final class SlowRenderingDetector {

    private final Duration slowRenderingDetectionPollInterval;

    SlowRenderingDetector(Duration slowRenderingDetectionPollInterval) {
        this.slowRenderingDetectionPollInterval = slowRenderingDetectionPollInterval;
    }

    /**
     * Installs the slow rendering detection instrumentation on the given {@link
     * InstrumentedApplication}.
     */
    public void install(Application application, OpenTelemetryRum openTelemetryRum) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "Slow/frozen rendering detection is not supported on platforms older than Android N (SDK version 24).");
            return;
        }

        SlowRenderListener detector =
                new SlowRenderListener(
                        openTelemetryRum
                                .getOpenTelemetry()
                                .getTracer("io.opentelemetry.slow-rendering"),
                        slowRenderingDetectionPollInterval);

        application.registerActivityLifecycleCallbacks(detector);
        detector.start();
    }
}
