/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.android.instrumentation.slowrendering;

import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.auto.service.AutoService;
import io.opentelemetry.android.common.RumConstants;
import io.opentelemetry.android.instrumentation.AndroidInstrumentation;
import io.opentelemetry.android.instrumentation.InstallationContext;
import java.time.Duration;

/** Entrypoint for installing the slow rendering detection instrumentation. */
@AutoService(AndroidInstrumentation.class)
public final class SlowRenderingInstrumentation implements AndroidInstrumentation {

    Duration slowRenderingDetectionPollInterval = Duration.ofSeconds(1);

    /**
     * Configures the rate at which frame render durations are polled.
     *
     * @param interval The period that should be used for polling.
     * @return {@code this}
     */
    public SlowRenderingInstrumentation setSlowRenderingDetectionPollInterval(Duration interval) {
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

    @Override
    public void install(@NonNull InstallationContext ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(
                    RumConstants.OTEL_RUM_LOG_TAG,
                    "Slow/frozen rendering detection is not supported on platforms older than Android N (SDK version 24).");
            return;
        }

        SlowRenderListener detector =
                new SlowRenderListener(
                        ctx.getOpenTelemetry().getTracer("io.opentelemetry.slow-rendering"),
                        slowRenderingDetectionPollInterval);

        ctx.getApplication().registerActivityLifecycleCallbacks(detector);
        detector.start();
    }
}
